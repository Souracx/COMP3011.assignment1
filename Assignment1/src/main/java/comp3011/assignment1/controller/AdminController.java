package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.model.UptimeResponse;
import comp3011.assignment1.service.UptimeService;


@RestController
@RequestMapping("/api/v1/admin")

public class AdminController {

	private final UptimeService uptimeService; 
	
	
	public AdminController(UptimeService uptimeService) { 
		this.uptimeService = uptimeService; 
	}
	
	/**
	 * Handles GET request to /api/v1/admin/uptime
	 * 
	 * @return Current server uptime information 
	 */
	
	@GetMapping("/uptime")
	public UptimeResponse getServerUptime() { 
		return uptimeService.currentUptime(); 
	}
}
