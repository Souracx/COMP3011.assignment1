package comp3011.assignment1.model;

/**
 * Represents global token usage statistics returned by /api/v1/global/stats endpoint 
 * 
 * @param inputTokens total input token consumed since server start
 * @param outputTokens total input token produced since server start
 */

public record GlobalStatResponse(
		long inputTokens, 
		long outputTokens) {
}
