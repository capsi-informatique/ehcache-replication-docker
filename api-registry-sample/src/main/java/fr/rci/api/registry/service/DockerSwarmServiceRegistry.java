package fr.rci.api.registry.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListTasksCmd;
import com.github.dockerjava.api.model.Task;
import com.github.dockerjava.api.model.TaskState;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;

import fr.rci.api.registry.config.model.DockerConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerSwarmServiceRegistry implements ServiceRegistry {

	private final DockerConfig config;
	
    private DockerClient dockerClient;

    @PostConstruct
    @SneakyThrows
    private final void init()  {
        try {
			dockerClient = DockerClientBuilder.getInstance()
            		.withDockerHttpClient(new ZerodepDockerHttpClient.Builder()
            				.dockerHost(new URI(config.getDockerHost()))
            				.connectionTimeout(config.getConnectionTimeout())
            				.responseTimeout(config.getResponseTimeout())
            				.maxConnections(config.getMaxConnections())
            				.build())
            		.build();
            dockerClient.pingCmd().exec();
        } catch (Throwable exception) {
            log.error("Error getting docker client. Error was: " + exception.getMessage(), exception);
        }
    }

    @Override
    public Map<String, List<String>> getServicePeers(String serviceName) {
        ListTasksCmd cmd = null ;
        if (serviceName != null) {
        	cmd = dockerClient.listTasksCmd().withServiceFilter(serviceName);
        }
        else {
        	cmd = dockerClient.listTasksCmd();
        }
        Map<String, List<String>> addresses = new HashMap<>() ;
        
        List<Task> tasks = cmd.exec();
        tasks.stream()
                .filter(t -> t.getStatus().getState().equals(TaskState.RUNNING))
                .forEach(t -> {
                	String name = t.getSpec().getContainerSpec().getLabels().get("com.docker.stack.namespace") ;;
                	name = name == null ? t.getId() : name ;
                	
					List<String> l = addresses.computeIfAbsent(name, n -> new ArrayList<String>()) ;
                	String hostName = t.getStatus().getContainerStatus().getContainerID().substring(0, 12) ;
                	String hostAddress;
					try {
						hostAddress = InetAddress.getByName(hostName).getHostAddress();
						l.add(hostAddress) ;
	                	log.debug("Address add {} for {}", hostAddress, name);  
					} catch (UnknownHostException e) {
						log.warn("Unable to resolve {}", hostName) ;
					}
                }) ;
        log.debug("Services are {}", addresses);
        
        return addresses;
    }
}
