package comp3011.assignment1.model;

/**
 * Response body for successfully accepted POST api/v1/admin/shutdown
 * @param readable message 
 */

public record ShutdownResponse(String message) {

}
