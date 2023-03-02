package net.sf.ehcache.distribution;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

public class ApiCacheManagerPeerProvider extends ContainerCacheManagerPeerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ApiCacheManagerPeerProvider.class.getName());

    private String serviceName;
    private String apiUrl;

    private Executor http;

    private ObjectMapper json;

    /**
     * Creates and starts a multicast peer provider
     *
     * @param groupMulticastAddress 224.0.0.1 to 239.255.255.255 e.g. 230.0.0.1
     * @param groupMulticastPort    1025 to 65536 e.g. 4446
     * @param hostAddress           the address of the interface to use for sending
     *                              and receiving multicast. May be null.
     */
    public ApiCacheManagerPeerProvider(CacheManager cacheManager, String serviceName, String apiUrl) {
        super(cacheManager);
        this.serviceName = serviceName;
        this.apiUrl = apiUrl;
    }

    /**
     * {@inheritDoc}
     */
    public final void init() throws CacheException {
        LOG.info("Initializing DockerCacheManagerPeerProvider with service name : {}", serviceName);
        try {
            super.init();
            http = Executor.newInstance() ;
            json = new ObjectMapper() ;
        } catch (Exception exception) {
            LOG.error("Error getting docker client. Error was: " + exception.getMessage(), exception);
            // throw new CacheException(exception.getMessage());
        }
    }

    public List<String> getOtherContainerAdresses() {
        LOG.debug("About to lookup container instance of {}", serviceName);
        List<String> ips = Collections.emptyList();
        try {
            ips = http.execute(Request.Get(apiUrl + "/" + serviceName)).handleResponse(new ResponseHandler<List<String>>() {
                @Override
                public List<String> handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    return ((Map<String, List<String>>) json.readValue(response.getEntity().getContent(), Map.class))
                            .entrySet().stream()
                            .filter(e -> serviceName.startsWith(e.getKey()))
                            .map(Entry::getValue)
                            .findFirst().get() ;
                }
            }).stream()
                .filter(ip -> !getHostAdress().equals(ip))
                .collect(Collectors.toList());

            LOG.debug("Found other container instance of {} : {}", serviceName, ips);
        } catch (IOException e) {
            LOG.warn("Unable to get service instance of {} with api {}", serviceName, apiUrl, e);
        }
        return ips;
    }

    @SneakyThrows
    public String mapHostNameToAdress(String hostName) {
        return InetAddress.getByName(hostName).getHostAddress();
    }

    /**
     * Shutdown the heartbeat
     */
    public final void dispose() {
        super.dispose();
    }
}
