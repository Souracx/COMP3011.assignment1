package comp3011.assignment1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Part of OpenAi transcription response 
 * 
 * @param text transcribed text 
 * @param usage token usage
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiTranscriptionResponse(
		String text, 
		Usage usage) {
	
	/**
	 * Token usage returned by OpenAI
	 * 
	 * @param inputTokens number of input tokens consumed
	 * @param outputTokens number of output tokens generaged
	 */
	public record Usage(
			
			@JsonProperty("input_tokens")
			Long inputTokens, 
			
			@JsonProperty("output_tokens")
			Long outputTokens) {}

}
