package comp3011.assignment1.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * converts given audio to text 
 * 
 * An interface so that the controller depends on the capability rather then OpenAI implementation
 */
public interface TranscriptionService {

	/**
	 * Transcibes given audio
	 * 
	 * @param audio the given audio file 
	 * @return transcribed text 
	 */
	String transcribe(MultipartFile audio); 
}
