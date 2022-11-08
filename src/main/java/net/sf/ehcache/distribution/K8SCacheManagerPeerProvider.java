package net.sf.ehcache.distribution;

import java.net.InetAddress;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

public class K8SCacheManagerPeerProvider extends ContainerCacheManagerPeerProvider {	
	private static final Logger LOG = LoggerFactory.getLogger(K8SCacheManagerPeerProvider.class.getName());
	private ApiClient client;

	private String k8sUrl;
	private String k8sToken;
	private String k8sUsername;
	private String k8sPassword;
	private String k8sNamespace;
	private String k8sAppSelector;
	private boolean validateSSL;

	public K8SCacheManagerPeerProvider(CacheManager cacheManager, String k8sAppSelector) {
		super(cacheManager);
		this.k8sAppSelector = k8sAppSelector;
	}

	public K8SCacheManagerPeerProvider(CacheManager cacheManager, String k8sUrl, String k8sToken, String k8sUsername, String k8sPassword,
			String k8sNamespace, String k8sAppSelector, boolean validateSSL) {
		this(cacheManager, k8sAppSelector);
		this.k8sUrl = k8sUrl;
		this.k8sToken = k8sToken;
		this.k8sUsername = k8sUsername;
		this.k8sPassword = k8sPassword;
		this.validateSSL = validateSSL;
		this.k8sNamespace = orDefault(k8sNamespace, "default");
	}

	private String orDefault(String val, String def) {
		return val != null && val.length() > 0 ? val : def ;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void init() throws CacheException {
		try {
			super.init();
			if (StringUtils.isNotBlank(k8sUrl) && StringUtils.isNotBlank(k8sUsername)) {
				client = Config.fromUserPassword(k8sUrl, k8sUsername, k8sPassword, validateSSL);
			}
			else if (StringUtils.isNotBlank(k8sUrl) && StringUtils.isNotBlank(k8sToken)) {
				client = Config.fromToken(k8sUrl, k8sToken, validateSSL);
			}
			else if (StringUtils.isNotBlank(k8sUrl) ) {
				client = Config.fromUrl(k8sUrl, validateSSL);
			}
			else {
				client = Config.defaultClient();
			}
		} catch (Exception exception) {
			LOG.error("Error getting docker client. Error was: " + exception.getMessage(), exception);
			// throw new CacheException(exception.getMessage());
		}
	}
	
	public List<String> getOtherContainerAdresses() {
		CoreV1Api api = new CoreV1Api(client);
		try {
			V1PodList items = api.listNamespacedPod(k8sNamespace, null, null, null, k8sAppSelector, null, null, null, null,
					10, false);
			List<String> serviceInstances = items.getItems().stream().map((pod) -> pod.getStatus().getPodIP())
					.map((ips) -> {
						InetAddress inet = null;
						try {
							inet = InetAddress.getByName(ips);
						} catch (Exception e) {
						}
						return inet;
					}).filter(c -> !getHostAdress().equals(c.getHostAddress())).map(c -> c.getHostAddress())
					.collect(Collectors.toList());

			LOG.debug("Found other container instance of {} : {}", k8sAppSelector, serviceInstances);
			return serviceInstances;
		} catch (Exception e) {
			LOG.error("Failed to retrieve pods list ", e);
			return Lists.newArrayList();
		}
	}
}
