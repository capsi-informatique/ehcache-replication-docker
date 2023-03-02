package fr.rci.api.registry.service;

import java.util.List;
import java.util.Map;

public interface ServiceRegistry {

	Map<String, List<String>> getServicePeers(String serviceName);

}
