package comp3011.assignment1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import comp3011.assignment1.model.TranscriptionResponse;
import comp3011.assignment1.service.TranscriptionService;

/**
 * Receives audio recordings from clients and returns
 * speech-to-text transcriptions.
 */
@RestController
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    public TranscriptionController(
            TranscriptionService transcriptionService) {

        this.transcriptionService = transcriptionService;
    }

    /**
     * Accepts an uploaded audio recording and transcribes it.
     *
     * @param audio recorded audio uploaded by the client
     * @return generated transcription
     */
    @PostMapping(
            value = "/api/v1/transcribe",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TranscriptionResponse transcribe(
            @RequestParam("audio") MultipartFile audio) {

        if (audio.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No audio data was supplied.");
        }

        String text =
                transcriptionService.transcribe(audio);

        return new TranscriptionResponse(text);
    }
}