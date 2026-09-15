package comp3011.assignment1.model;

/**
 * Response body returned to browser after a successful transcription
 * 
 * @param text transcribed text produced from audio 
 */
public record TranscriptionResponse(String text) {

}
