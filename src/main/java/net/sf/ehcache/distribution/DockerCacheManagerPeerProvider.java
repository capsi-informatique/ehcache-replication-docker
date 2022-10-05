package net.sf.ehcache.distribution;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectServiceCmd;
import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.core.DockerClientBuilder;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

public class DockerCacheManagerPeerProvider extends RMICacheManagerPeerProvider {
	/**
	 * One tenth of a second, in ms
	 */
	protected static final int SHORT_DELAY = 100;

	private static final Logger LOG = LoggerFactory.getLogger(DockerCacheManagerPeerProvider.class.getName());
	private DockerClient dockerClient;

	private ScheduledExecutorService executor;

	private String serviceName;

	private String hostName;

	private String hostAdress;

	/**
	 * Creates and starts a multicast peer provider
	 *
	 * @param groupMulticastAddress 224.0.0.1 to 239.255.255.255 e.g. 230.0.0.1
	 * @param groupMulticastPort    1025 to 65536 e.g. 4446
	 * @param hostAddress           the address of the interface to use for sending
	 *                              and receiving multicast. May be null.
	 */
	public DockerCacheManagerPeerProvider(CacheManager cacheManager, String serviceName) {
		super(cacheManager);
		this.serviceName = serviceName;

	}

	/**
	 * {@inheritDoc}
	 */
	public final void init() throws CacheException {
		try {
			hostName = InetAddress.getLocalHost().getHostName();
			hostAdress = InetAddress.getLocalHost().getHostAddress();
			dockerClient = DockerClientBuilder.getInstance().build();
			dockerClient.pingCmd().exec();
			executor = Executors.newScheduledThreadPool(1);
			executor.scheduleWithFixedDelay(this::registerDockerPeers, 1000, 1000, TimeUnit.MILLISECONDS);
		} catch (Exception exception) {
			LOG.error("Error getting docker client. Error was: " + exception.getMessage(), exception);
			// throw new CacheException(exception.getMessage());
		}
	}

	private void registerDockerPeers() {
		LOG.debug("About to register Docker CachePeer for service {} on host {}", serviceName, hostName);

		CacheManagerPeerListener cacheManagerPeerListener = cacheManager.getCachePeerListener("RMI");
		if (cacheManagerPeerListener == null) {
			LOG.warn(
					"The RMICacheManagerPeerListener is missing. You need to configure a cacheManagerPeerListenerFactory"
							+ " with class=\"net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory\" in ehcache.xml.");
			return;
		}
		List<String> otherContainerNames = getOtherContainerAdresses();
		List<CachePeer> localCachePeers = cacheManagerPeerListener.getBoundCachePeers();
		localCachePeers.stream().map(p -> {
			try {
				return p.getUrl();
			} catch (RemoteException e) {
				return null;
			}
		}).filter(url -> url != null).forEach(url -> {
			LOG.debug("About to register docker cachePeers for local url {}", url);
			otherContainerNames.forEach(otherHosts -> {
				String otherUrl = "rmi:" + url.replace(hostAdress, otherHosts);
				if (!peerUrls.containsKey(otherUrl)) {
					LOG.debug("Registering target docker CachePeer ", otherUrl);
					registerPeer(otherUrl);
				}
			});
		});
	}

    public List<String> getOtherContainerAdresses() {
        InspectServiceCmd inspectService = dockerClient.inspectServiceCmd(serviceName) ;
        Service service = inspectService.exec() ;
        List<String> serviceInstances = Arrays.asList(service.getEndpoint().getVirtualIPs()).stream()
                    .map( evip -> {
	                            InetAddress inet = null ;
			                    try {
			                          inet = InetAddress.getByName(evip.getAddr()) ;
			                    } catch( Exception e ) {}
			                    return inet ;
		                    }
                    )
                    .filter(c -> ! hostAdress.equals( c.getHostAddress() ) )
                    .map( c -> c.getHostAddress() )
                    .collect( Collectors.toList() ) ;
              LOG.debug("Found other container instance of {} : {}", serviceName, serviceInstances);
              return serviceInstances ;
      }


	/**
	 * Register a new peer, but only if the peer is new, otherwise the last seen
	 * timestamp is updated.
	 * <p>
	 * This method is thread-safe. It relies on peerUrls being a synchronizedMap
	 *
	 * @param rmiUrl
	 */
	public final void registerPeer(String rmiUrl) {
		try {
			CachePeerEntry cachePeerEntry = (CachePeerEntry) peerUrls.get(rmiUrl);
			if (cachePeerEntry == null || stale(cachePeerEntry.date)) {
				// can take seconds if there is a problem
				CachePeer cachePeer = lookupRemoteCachePeer(rmiUrl);
				cachePeerEntry = new CachePeerEntry(cachePeer, new Date());
				// synchronized due to peerUrls being a synchronizedMap
				peerUrls.put(rmiUrl, cachePeerEntry);
			} else {
				cachePeerEntry.date = new Date();
			}
		} catch (IOException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unable to lookup remote cache peer for " + rmiUrl + ". Removing from peer list. Cause was: "
						+ e.getMessage());
			}
			unregisterPeer(rmiUrl);
		} catch (NotBoundException e) {
			peerUrls.remove(rmiUrl);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unable to lookup remote cache peer for " + rmiUrl + ". Removing from peer list. Cause was: "
						+ e.getMessage());
			}
		} catch (Throwable t) {
			LOG.error("Unable to lookup remote cache peer for " + rmiUrl
					+ ". Cause was not due to an IOException or NotBoundException which will occur in normal operation:"
					+ " " + t.getMessage());
		}
	}

	/**
	 * @return a list of {@link CachePeer} peers, excluding the local peer.
	 */
	public final synchronized List listRemoteCachePeers(Ehcache cache) throws CacheException {
		List remoteCachePeers = new ArrayList();
		List staleList = new ArrayList();
		synchronized (peerUrls) {
			for (Iterator iterator = peerUrls.keySet().iterator(); iterator.hasNext();) {
				String rmiUrl = (String) iterator.next();
				String rmiUrlCacheName = extractCacheName(rmiUrl);
				try {
					if (!rmiUrlCacheName.equals(cache.getName())) {
						continue;
					}
					CachePeerEntry cachePeerEntry = (CachePeerEntry) peerUrls.get(rmiUrl);
					Date date = cachePeerEntry.date;
					if (!stale(date)) {
						CachePeer cachePeer = cachePeerEntry.cachePeer;
						remoteCachePeers.add(cachePeer);
					} else {

						LOG.debug("rmiUrl is stale. Either the remote peer is shutdown or the "
								+ "network connectivity has been interrupted. Will be removed from list of remote cache peers",
								rmiUrl);
						staleList.add(rmiUrl);
					}
				} catch (Exception exception) {
					LOG.error(exception.getMessage(), exception);
					throw new CacheException("Unable to list remote cache peers. Error was " + exception.getMessage());
				}
			}
			// Must remove entries after we have finished iterating over them
			for (int i = 0; i < staleList.size(); i++) {
				String rmiUrl = (String) staleList.get(i);
				peerUrls.remove(rmiUrl);
			}
		}
		return remoteCachePeers;
	}

	/**
	 * Shutdown the heartbeat
	 */
	public final void dispose() {
		try {
			executor.shutdown();
			dockerClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Time for a cluster to form. This varies considerably, depending on the
	 * implementation.
	 *
	 * @return the time in ms, for a cluster to form
	 */
	public long getTimeForClusterToForm() {
		return 2000 + SHORT_DELAY;
	}

	/**
	 * The time after which an unrefreshed peer provider entry is considered stale.
	 */
	protected long getStaleTime() {
		return MulticastKeepaliveHeartbeatSender.getHeartBeatStaleTime();
	}

	/**
	 * Whether the entry should be considered stale. This will depend on the type of
	 * RMICacheManagerPeerProvider. This method should be overridden for
	 * implementations that go stale based on date
	 *
	 * @param date the date the entry was created
	 * @return true if stale
	 */
	protected final boolean stale(Date date) {
		long now = System.currentTimeMillis();
		return date.getTime() < (now - getStaleTime());
	}

	/**
	 * Entry containing a looked up CachePeer and date
	 */
	protected static final class CachePeerEntry {

		private final CachePeer cachePeer;
		private Date date;

		/**
		 * Constructor
		 *
		 * @param cachePeer the cache peer part of this entry
		 * @param date      the date part of this entry
		 */
		public CachePeerEntry(CachePeer cachePeer, Date date) {
			this.cachePeer = cachePeer;
			this.date = date;
		}

		/**
		 * @return the cache peer part of this entry
		 */
		public final CachePeer getCachePeer() {
			return cachePeer;
		}

		/**
		 * @return the date part of this entry
		 */
		public final Date getDate() {
			return date;
		}

	}
}
