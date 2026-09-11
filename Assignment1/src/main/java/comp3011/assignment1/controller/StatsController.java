package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.model.GlobalStatResponse;
import comp3011.assignment1.service.TokenUsageTracker;

/**
 * Rest controller providing global token usage stats for speech to text service 
 */

@RestController
@RequestMapping("/api/v1/global")
public class StatsController {

	private final TokenUsageTracker tokenUsageTracker; 
	
	public StatsController(TokenUsageTracker tokenUsageTracker) { 
		this.tokenUsageTracker = tokenUsageTracker; 
	}
	
	@GetMapping("/stats")
	public GlobalStatResponse getGlobalStats() {
		return tokenUsageTracker.snapshot(); 
	}
}
