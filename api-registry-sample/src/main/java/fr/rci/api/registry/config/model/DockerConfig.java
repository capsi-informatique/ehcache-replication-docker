package fr.rci.api.registry.config.model;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties("docker")
public class DockerConfig {
    private String dockerHost = "unix:///var/run/docker.sock";

    private int maxConnections = 20;

    private Duration connectionTimeout = Duration.ofMillis(1000);

    private Duration responseTimeout = Duration.ofMillis(10000);

}
