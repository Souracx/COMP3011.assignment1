package comp3011.assignment1.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import comp3011.assignment1.model.OpenAiTranscriptionResponse;

/**
 * Transcribes uploaded audio using the OpenAI speech to text api
 */
@Service
public class OpenAiTranscriptionService implements TranscriptionService {
	
    //Test case logger
    private static final Logger log = LoggerFactory.getLogger(OpenAiTranscriptionService.class); 

    private final RestClient openAiRestClient;
    private final TokenUsageTracker tokenUsageTracker;
    private final String apiUrl;
    private final String model;

    public OpenAiTranscriptionService(
            RestClient openAiRestClient,
            TokenUsageTracker tokenUsageTracker,
            @Value("${openai.api.url}") String apiUrl,
            @Value("${openai.api.model}") String model) {

        this.openAiRestClient = openAiRestClient;
        this.tokenUsageTracker = tokenUsageTracker;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    /**
     * Sends the uploaded audio to OpenAI and returns the generated text.
     *
     * @param audio uploaded audio recording
     * @return transcription returned by OpenAI
     */
    @Override
    public String transcribe(MultipartFile audio) {

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

        parts.add("model", model);
        parts.add("response_format", "json");
        parts.add("file", createFilePart(audio));

        try {
            OpenAiTranscriptionResponse response =
                    openAiRestClient.post()
                            .uri(apiUrl)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(parts)
                            .retrieve()
                            .body(OpenAiTranscriptionResponse.class);

            if (response == null || response.text() == null || response.text().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Cloud transcription service returned no transcription.");
            }

            recordUsage(response);

            return response.text();

        } catch (RestClientResponseException e) {
        	
        	log.warn("OpenAI rejected the request with status{}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Cloud transcription service returned status " + e.getStatusCode().value());

        } catch (ResourceAccessException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Could not reach the Cloud transcription service.");
        }
    }

    /**
     * Creates the multipart file section sent to OpenAI.
     */
    private HttpEntity<Resource> createFilePart(MultipartFile audio) {

        HttpHeaders headers = new HttpHeaders();

        String filename = safeFilename(audio);

        headers.setContentDispositionFormData("file", filename);

        String suppliedContentType = audio.getContentType();

        if (suppliedContentType != null && !suppliedContentType.isBlank()) {

            try {
                headers.setContentType(
                        MediaType.parseMediaType(suppliedContentType));
            } catch (IllegalArgumentException e) {
                headers.setContentType(
                        MediaType.APPLICATION_OCTET_STREAM);
            }

        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        return new HttpEntity<>(audio.getResource(), headers);
    }
    


    /**
     * Records any token usage returned by the Cloud service.
     */
    private void recordUsage(OpenAiTranscriptionResponse response) {

        if (response.usage() == null) {
        	log.info("OpenAI returned no usage block; token totals were not updated"); 
            return;
        }

        long inputTokens; 
        if (response.usage().inputTokens() == null) { 
        	inputTokens = 0; 
        }else { 
        	inputTokens = response.usage().inputTokens(); 
        }
        
        long outputTokens; 
        if (response.usage().outputTokens() ==null) { 
        	outputTokens = 0; 
        }else { 
        	outputTokens = response.usage().outputTokens(); 
        }
        
        tokenUsageTracker.recordUsage(inputTokens, outputTokens); 
    }

    /**
     * Produces a safe filename for the multipart upload.
     */
    private String safeFilename(MultipartFile audio) {

        String original = audio.getOriginalFilename();

        if (original == null || original.isBlank()) {
            return "recording.webm";
        }

        String normalized =
                original.replace('\\', '/');

        String filename =
                normalized.substring(
                        normalized.lastIndexOf('/') + 1);

        if (filename.isBlank()
                || filename.contains("..")) {

            return "recording.webm";
        }

        return filename;
    }
}