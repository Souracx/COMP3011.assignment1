package comp3011.assignment1.model;

import java.time.Instant;

/**
 * Standard JSON response body defined by error response 
 */

public record ErrorResponse(
		Instant timestamp, 
		int status, 
		String error, 
		String message, 
		String path) {

}
