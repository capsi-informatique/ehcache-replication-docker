package fr.rci.api.registry.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.rci.api.registry.service.ServiceRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "service registry controller")
public class ServiceRegistryController {

	@Autowired 
	private ServiceRegistry srv ;
	
	public ServiceRegistryController() {
	}

	@GetMapping("/api/services")
	@Operation(summary = "list services")
	public @ResponseBody ResponseEntity<?> allServices() {
		return ResponseEntity.ok(srv.getServicePeers(null));
	}

	@GetMapping("/api/services/{name}")
	@Operation(summary = "list services")
	public @ResponseBody ResponseEntity<?> service(@PathVariable("name") String serviceName) {
		return ResponseEntity.ok(srv.getServicePeers(serviceName));
	}

}