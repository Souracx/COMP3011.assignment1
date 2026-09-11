package comp3011.assignment1.service;

import comp3011.assignment1.model.UptimeResponse;
import jakarta.annotation.*;
import org.springframework.stereotype.Service;

import java.time.*;

/**
 * Tracks server start time and compute uptime 
 * for the api/v1/admin/admin endpoint
 */


@Service
public class UptimeService { 
	
	private Instant serverStart; 
	
	//Record server start time once, when it's created during startup 
	@PostConstruct
	public void init() { 
		this.serverStart = Instant.now(); 
	}
	
	// Build current uptime snapshot 
	public UptimeResponse currentUptime() { 
		Instant now = Instant.now(); 
		double uptimeSeconds = Duration.between(serverStart, now).toNanos() / 1_000_000_000.0; 
		return new UptimeResponse(serverStart, now, uptimeSeconds); 
	}

}
