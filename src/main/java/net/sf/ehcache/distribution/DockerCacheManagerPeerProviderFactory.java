package net.sf.ehcache.distribution;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.util.PropertyUtil;

public class DockerCacheManagerPeerProviderFactory extends CacheManagerPeerProviderFactory {
	private static final Logger LOG = LoggerFactory.getLogger(DockerCacheManagerPeerProviderFactory.class.getName());

	public DockerCacheManagerPeerProviderFactory() {
	}

	@Override
	public CacheManagerPeerProvider createCachePeerProvider(CacheManager cacheManager, Properties properties) {
        String dockerSwarmServiceName = PropertyUtil.extractAndLogProperty("dockerSwarmServiceName", properties);
        String apiServiceName = PropertyUtil.extractAndLogProperty("apiServiceName", properties);
        String apiUrl = PropertyUtil.extractAndLogProperty("apiUrl", properties);
        String k8sUrl = PropertyUtil.extractAndLogProperty("k8sUrl", properties);
        String k8sUsername = PropertyUtil.extractAndLogProperty("k8sUsername", properties);
        String k8sPassword = PropertyUtil.extractAndLogProperty("k8sPassword", properties);
        String k8sNamespace = PropertyUtil.extractAndLogProperty("k8sNamespace", properties);
        String k8sAppSelector = PropertyUtil.extractAndLogProperty("k8sAppSelector", properties);
        String k8sToken = PropertyUtil.extractAndLogProperty("k8sToken", properties);
        String k8sValidateSSL = PropertyUtil.extractAndLogProperty("k8sValidateSSL", properties);
        
        if (StringUtils.isNotBlank(apiServiceName) && StringUtils.isNotBlank(apiServiceName)) {
            return new ApiCacheManagerPeerProvider(cacheManager, apiServiceName, apiUrl);
        }
        else if (StringUtils.isNotBlank(dockerSwarmServiceName)) {
        	return new DockerCacheManagerPeerProvider(cacheManager, dockerSwarmServiceName);
		}
		else if (StringUtils.isNotBlank(k8sAppSelector)) {
        	boolean validateSSL = StringUtils.isNotBlank(k8sValidateSSL) ? Boolean.parseBoolean(k8sValidateSSL) : true ;
			return new K8SCacheManagerPeerProvider(cacheManager, k8sUrl, k8sToken, k8sUsername, k8sPassword, k8sNamespace, k8sAppSelector, validateSSL);
		}
		else {
			LOG.error("No dockerSwarmServiceName nor k8sAppSelector configured, no CacheManagerPeerProvider can be created");
			return null;
		}
	}
}
