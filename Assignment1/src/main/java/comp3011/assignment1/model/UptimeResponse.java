package comp3011.assignment1.model;

import java.time.Instant;

/**
 * Represent server uptime information returned by {/api/v1/admin/uptime} endpoint 
 * 
 * @param utcServerStart 	the UTC timestamp when server starts
 * @param utcNow			the current UTC timestamp when response is generated
 * @param severUptimeSeconds the number of seconds Server been running 
 */

//Create an uptime response record 
public record UptimeResponse(
        Instant utcServerStart,
        Instant utcNow,
        double serverUptimeSeconds) {
}