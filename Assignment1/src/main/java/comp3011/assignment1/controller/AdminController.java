package comp3011.assignment1.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.model.ErrorResponse;
import comp3011.assignment1.model.ShutdownResponse;
import comp3011.assignment1.model.UptimeResponse;
import comp3011.assignment1.service.ShutdownService;
import comp3011.assignment1.service.UptimeService;


/**
 * Controller for admin server operations 
 */

@RestController
@RequestMapping("/api/v1/admin")

public class AdminController {

	private final UptimeService uptimeService; 
	private final ShutdownService shutdownService; 
	
	//Constructor 
	public AdminController(UptimeService uptimeService, ShutdownService shutdownService) { 
		this.uptimeService = uptimeService; 
		this.shutdownService = shutdownService; 
		
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
	
	
	/**
	 * Request a graceful shutdown of server 
	 * 
	 * @return 202 if this call initiated the shutdown, 
	 * or 409 if a shutdown was already in progress
	 */
	@PostMapping("/shutdown")
	public ResponseEntity<?> shutdownServer(){ 
		// returns true only for the first request that successfully initiates the shutdown 
		if (shutdownService.requestShutdown()) {
			return ResponseEntity
					.accepted() 
					.body(new ShutdownResponse("Graceful shutdown requested.")); 
		}
		
		//if returns false another shutdown request has already been accepted
		return ResponseEntity 
				.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse(
						Instant.now(), 
						409, 
						"Conflict", 
						"Graceful shutdown already in progress.", 
						"/api/v1/admin/shutdown")); 
	}
}
