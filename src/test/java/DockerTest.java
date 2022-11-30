import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectServiceCmd;
import com.github.dockerjava.api.command.ListServicesCmd;
import com.github.dockerjava.api.command.ListTasksCmd;
import com.github.dockerjava.api.model.EndpointVirtualIP;
import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.Task;
import com.github.dockerjava.api.model.TaskState;
import com.github.dockerjava.core.DockerClientBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Disabled
public class DockerTest {

	static DockerClient dockerClient;
	static String serviceName = "apis-api-articles_app";

	static {
		dockerClient = DockerClientBuilder.getInstance("tcp://localhost:2375").build();
		dockerClient.pingCmd().exec();
	}

	@Test
	public void testTaks() {
		log.info("About to lookup container instance of " + serviceName);
		ListTasksCmd cmd = dockerClient.listTasksCmd().withServiceFilter(serviceName);
		List<Task> tasks = cmd.exec();
		List<String> ips = 
				tasks.stream().filter(t -> t.getStatus().getState().equals(TaskState.RUNNING))
				.map(t -> t.getStatus().getContainerStatus().getContainerID().substring(0, 12))
				.collect(Collectors.toList());

		log.info("Found other container instance of " + serviceName + " : " + ips);
	}

	@Test
	public void testService() {
		InspectServiceCmd inspectService = dockerClient.inspectServiceCmd(serviceName);
		Service service = inspectService.exec();
		log.info("Found service : " + service);
		EndpointVirtualIP[] virtualIPs = service.getEndpoint().getVirtualIPs();
		log.info("Service virtual ips : " + virtualIPs);
		List<String> serviceInstances = Arrays.asList(virtualIPs).stream().map(evip -> {
			InetAddress inet = null;
			String name = evip.getAddr().split("/")[0];
			try {
				inet = InetAddress.getByName(name);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return inet;
		}).map(c -> c.getHostAddress()).collect(Collectors.toList());
		log.info("container instance " + serviceInstances);
	}

	@Test
	public void listService() {
		ListServicesCmd cmd = dockerClient.listServicesCmd();
		List<Service> services = cmd.exec();
		services.forEach(System.out::println);
	}

}
