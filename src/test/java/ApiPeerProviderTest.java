import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.distribution.ApiCacheManagerPeerProvider;

public class ApiPeerProviderTest {

	@Test
	public void test() {
		String serviceName = "apis-api-registry_app";
        System.out.println("About to lookup container instance of " + serviceName );
		ApiCacheManagerPeerProvider api = new ApiCacheManagerPeerProvider(CacheManager.getInstance(), serviceName, "https://staging.routhiau.fr/api-registry/api/services");
		api.init() ;
		List<String> ips = api.getOtherContainerAdresses() ;
		System.out.println("Found other container instance of " + serviceName + " : " + ips);
		Assertions.assertFalse(ips.isEmpty());
	}


}
