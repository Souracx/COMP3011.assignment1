package comp3011.assignment1.service;

import org.springframework.boot.SpringApplication;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;


/**
 * Handles request for server shutdown 
 */

@Service
public class ShutdownService {

	private final ApplicationContext applicationContext ; 
	
	//Keeps track of whether a shutdown has already been requested 
	private final AtomicBoolean shutdownRequested = new AtomicBoolean(false); 
	
	public ShutdownService(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext; 
	
	}
	
	/**
	 * Attempt to begin a graceful shutdown 
	 * 
	 * @return true if this call initiated the shutdown, 
	 * false if a shutdown was already in progress
	 */
	public boolean requestShutdown() { 
		
		//Change false -> true atomically 
		//If it's already true, another request has already started shutdown 
		if (!shutdownRequested.compareAndSet(false, true)) {
			return false; 
	}
	
	//Run actual shutdown on a separate virtual thread
    Thread.ofVirtual().name("shutdown-trigger").start(() -> {
        try {
        	//Give HTTP response time to reach client 
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        //Tell spring boot to shutdown
        SpringApplication.exit(applicationContext, () -> 0);
    });

    return true;
	}
}