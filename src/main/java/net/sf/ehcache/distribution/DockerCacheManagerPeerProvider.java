package net.sf.ehcache.distribution;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListTasksCmd;
import com.github.dockerjava.api.model.Task;
import com.github.dockerjava.api.model.TaskState;
import com.github.dockerjava.core.DockerClientBuilder;

import lombok.SneakyThrows;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

public class DockerCacheManagerPeerProvider extends ContainerCacheManagerPeerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DockerCacheManagerPeerProvider.class.getName());
    private DockerClient dockerClient;

    private String serviceName;

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
        LOG.info("Initializing DockerCacheManagerPeerProvider with service name : {}", serviceName);
        try {
            super.init();
            dockerClient = DockerClientBuilder.getInstance().build();
            dockerClient.pingCmd().exec();
        } catch (Exception exception) {
            LOG.error("Error getting docker client. Error was: " + exception.getMessage(), exception);
            // throw new CacheException(exception.getMessage());
        }
    }

    public List<String> getOtherContainerAdresses() {
        LOG.debug("About to lookup container instance of {}", serviceName);
        ListTasksCmd cmd = dockerClient.listTasksCmd().withServiceFilter(serviceName);
        List<Task> tasks = cmd.exec();
        List<String> ips = tasks.stream()
                .filter(t -> t.getStatus().getState().equals(TaskState.RUNNING))
                .map(t -> t.getStatus().getContainerStatus().getContainerID().substring(0, 12))
                .filter(hostName -> !getHostName().equals(hostName))
                .map(this::mapHostNameToAdress)
                .collect(Collectors.toList());

        LOG.debug("Found other container instance of {} : {}", serviceName, ips);
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
        try {
            dockerClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
