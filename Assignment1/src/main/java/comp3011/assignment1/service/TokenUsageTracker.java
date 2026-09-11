package comp3011.assignment1.service;

import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Service;

import comp3011.assignment1.model.GlobalStatResponse;

@Service 
public class TokenUsageTracker {

	private final LongAdder inputTokens = new LongAdder(); 
	private final LongAdder outputTokens = new LongAdder(); 
	
	/**
	 * Adds token usage from completed text to speech record
	 * @param input  number of input tokens used
	 * @param output number of output tokens produced 
	 */
	
	public void recordUsage(long input, long output) { 
		inputTokens.add(input);
		outputTokens.add(output); 
	}
	
	/**
	 * Returns the current totals
	 */
	public GlobalStatResponse snapshot() { 
		return new GlobalStatResponse(inputTokens.sum(), outputTokens.sum()); 
	}
}
