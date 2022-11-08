package net.sf.ehcache.distribution;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectServiceCmd;
import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.core.DockerClientBuilder;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

public class DockerCacheManagerPeerProvider extends ContainerCacheManagerPeerProvider {


    private static final Logger LOG = LoggerFactory.getLogger(DockerCacheManagerPeerProvider.class.getName());
    private DockerClient dockerClient ;


	private String serviceName;

    /**
     * Creates and starts a multicast peer provider
     *
     * @param groupMulticastAddress 224.0.0.1 to 239.255.255.255 e.g. 230.0.0.1
     * @param groupMulticastPort    1025 to 65536 e.g. 4446
     * @param hostAddress the address of the interface to use for sending and receiving multicast. May be null.
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
        	super.init(); 
        	dockerClient = DockerClientBuilder.getInstance().build();
        	dockerClient.pingCmd().exec() ;
        } catch (Exception exception) {
            LOG.error("Error getting docker client. Error was: " + exception.getMessage(), exception);
            //throw new CacheException(exception.getMessage());
        }
    }
    
    public List<String> getOtherContainerAdresses() {    	    
    	InspectServiceCmd inspectService = dockerClient.inspectServiceCmd(serviceName) ;
    	Service service = inspectService.exec() ;       
    	List<String> serviceInstances = Arrays.asList(service.getEndpoint().getVirtualIPs()).stream()
    			.map( evip -> {
    				InetAddress inet = null ;
    	    		try {
    	    			inet = InetAddress.getByName(evip.getAddr()) ;
    	    		} catch( Exception e ) {    	    			
    	    		}
    	    		return inet ;
    			})
    			.filter(c -> ! getHostAdress().equals( c.getHostAddress() ) )
    			.map( c -> c.getHostAddress() )
    			.collect( Collectors.toList() ) ;
		LOG.debug("Found other container instance of {} : {}", serviceName, serviceInstances);
		return serviceInstances ;
    }
 
  
    /**
     * Shutdown the heartbeat
     */
    public final void dispose() {
    	super.dispose();
    	try {
    		dockerClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
