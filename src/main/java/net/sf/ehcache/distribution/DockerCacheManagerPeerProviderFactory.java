package net.sf.ehcache.distribution;

import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.util.PropertyUtil;

public class DockerCacheManagerPeerProviderFactory extends CacheManagerPeerProviderFactory {

	public DockerCacheManagerPeerProviderFactory() {
	}

	@Override
	public CacheManagerPeerProvider createCachePeerProvider(CacheManager cacheManager, Properties properties) {
        String dockerSwarmServiceName = PropertyUtil.extractAndLogProperty("dockerSwarmServiceName", properties);
        if (dockerSwarmServiceName != null && dockerSwarmServiceName.length() > 0) {
        	return new DockerCacheManagerPeerProvider(cacheManager, dockerSwarmServiceName);
		}
		else {
			return null;
		}
	}

}
