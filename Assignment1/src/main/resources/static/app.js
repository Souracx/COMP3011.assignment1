/*
 * Client-side recording and transcription.
 *
 * A single toggle button starts and stops recording. While recording, the waveform bars react to live microphone 
 * input and a timer counts up. The
 * captured audio is uploaded to the backend transcription endpoint and the
 * result is displayed with word count, recording length and response time.
 */

"use strict";

const TRANSCRIBE_URL = "/api/v1/transcribe";


const RECORDER_BITRATE = 32000;

const toggleButton = document.getElementById("recordToggle");
const toggleLabel = document.getElementById("recordLabel");
const timerElement = document.getElementById("timer");
const statusElement = document.getElementById("status");
const transcriptElement = document.getElementById("transcript");
const copyButton = document.getElementById("copyButton");
const copyLabel = document.getElementById("copyLabel");
const metaList = document.getElementById("transcriptMeta");
const metaWords = document.getElementById("metaWords");
const metaDuration = document.getElementById("metaDuration");
const metaLatency = document.getElementById("metaLatency");
const bars = Array.from(toggleButton.querySelectorAll(".bar"));

/*
 * Every UI state in one table. setState() is the only place the recorder
 * changes, so the button, label, timer and status can never disagree.
 */
const STATES = {
    idle: {
        label: "Start Recording",
        status: "Ready to record.",
        disabled: false
    },
    starting: {
        label: "Start Recording",
        status: "Waiting for microphone access…",
        disabled: true
    },
    recording: {
        label: "Stop Recording",
        status: "Recording. Press Stop Recording when you're finished.",
        disabled: false
    },
    processing: {
        label: "Transcribing…",
        status: "Transcribing your recording…",
        disabled: true
    }
};

let currentState = "idle";
let mediaStream = null;
let mediaRecorder = null;
let audioChunks = [];
let audioContext = null;
let animationFrameId = null;
let timerIntervalId = null;
let recordingStartedAt = 0;
let recordingSeconds = 0;
let copyResetId = null;

/**
 * Moves the page into a new state. An optional message replaces the default status text.
 */
function setState(nextState, statusMessage) {
    const config = STATES[nextState];
    currentState = nextState;

    toggleButton.dataset.state = nextState;
    toggleButton.disabled = config.disabled;
    toggleLabel.textContent = config.label;

    timerElement.dataset.state = nextState;
    if (nextState === "idle") {
        timerElement.textContent = "00:00";
    }

    statusElement.dataset.state = nextState;
    statusElement.textContent = statusMessage ?? config.status;
}

/**
 * Returns the page to idle and shows an error in the status line.
 */
function showError(message) {
    setState("idle", message);
    statusElement.dataset.state = "error";
}

/**
 * The single click handler: what a click does depends on the current state.
 */
function handleToggleClick() {
    if (currentState === "idle") {
        startRecording();
    } else if (currentState === "recording") {
        stopRecording();
    }
    // "starting" and "processing" disable the button, so clicks are ignored.
}

/**
 * Timer 
 */

function formatClock(totalSeconds) {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = Math.floor(totalSeconds % 60);
    return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
}

function startTimer() {
    recordingStartedAt = performance.now();
    timerElement.textContent = "00:00";
    timerIntervalId = setInterval(() => {
        timerElement.textContent = formatClock((performance.now() - recordingStartedAt) / 1000);
    }, 250);
}

function stopTimer() {
    clearInterval(timerIntervalId);
    timerIntervalId = null;
    recordingSeconds = (performance.now() - recordingStartedAt) / 1000;
}

/**
 * Recording - Chooses a recording format the current browser supports.
*/
function pickMimeType() {
    const candidates = [
        "audio/webm;codecs=opus",
        "audio/webm",
        "audio/mp4",
        "audio/ogg;codecs=opus"
    ];
    return candidates.find((type) => MediaRecorder.isTypeSupported(type)) || "";
}

/**
 * Gives the upload a file extension that matches the real audio format, so the transcription API can identify it.
 */
function fileNameFor(mimeType) {
    if (mimeType.includes("mp4")) {
        return "recording.mp4";
    }
    if (mimeType.includes("ogg")) {
        return "recording.ogg";
    }
    return "recording.webm";
}

/**
 * Requests microphone access and begins recording.
 */
async function startRecording() {
    // Disable the button immediately so a double click can't start two recordings.
    setState("starting");

    try {
        mediaStream = await navigator.mediaDevices.getUserMedia({
            audio: {
                echoCancellation: true,
                noiseSuppression: true,
                channelCount: 1
            }
        });
    } catch (error) {
        if (error.name === "NotAllowedError") {
            showError("Microphone access is blocked. Allow it in your browser's site settings, then try again.");
        } else if (error.name === "NotFoundError") {
            showError("No microphone was found. Connect one, then try again.");
        } else {
            showError("The microphone couldn't be started. Check it isn't in use by another app, then try again.");
        }
        return;
    }

    const mimeType = pickMimeType();
    const recorderOptions = { audioBitsPerSecond: RECORDER_BITRATE };
    if (mimeType) {
        recorderOptions.mimeType = mimeType;
    }

    audioChunks = [];
    mediaRecorder = new MediaRecorder(mediaStream, recorderOptions);

    mediaRecorder.addEventListener("dataavailable", (event) => {
        if (event.data.size > 0) {
            audioChunks.push(event.data);
        }
    });

    mediaRecorder.addEventListener("stop", handleRecordingStopped, { once: true });

    mediaRecorder.start();
    startVisualiser(mediaStream);
    startTimer();
    setState("recording");
}

/**
 * Stops the recorder. Uploading happens once the recorder fires its "stop" event.
 */
function stopRecording() {
    stopTimer();
    setState("processing");
    stopVisualiser();

    if (mediaRecorder && mediaRecorder.state !== "inactive") {
        mediaRecorder.stop();
    }
}

/**
 * Called once the recorder has flushed its final data; uploads the audio.
 */
async function handleRecordingStopped() {
    releaseMicrophone();

    const mimeType = mediaRecorder.mimeType || "audio/webm";
    const audioBlob = new Blob(audioChunks, { type: mimeType });

    if (audioBlob.size === 0) {
        showError("No audio was captured. Record for at least a second, then try again.");
        return;
    }

    await uploadForTranscription(audioBlob, fileNameFor(mimeType));
}

/**
 * Stops all microphone tracks so the browser's recording indicator clears.
 */
function releaseMicrophone() {
    if (mediaStream) {
        mediaStream.getTracks().forEach((track) => track.stop());
        mediaStream = null;
    }
}

/*Upload and display*/

/**
 * Sends the recorded audio to the backend and displays the transcription.
 */
async function uploadForTranscription(audioBlob, fileName) {
    const formData = new FormData();
    // The field name must match @RequestParam("audio") on the server.
    formData.append("audio", audioBlob, fileName);

    const requestStartedAt = performance.now();

    try {
        const response = await fetch(TRANSCRIBE_URL, {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            const detail = await response.json().catch(() => ({}));
            throw new Error(detail.message || "Transcription failed (HTTP " + response.status + "). Record again to retry.");
        }

        const result = await response.json();
        const responseSeconds = (performance.now() - requestStartedAt) / 1000;
        const text = (result.text || "").trim();

        if (text.length > 0) {
            showTranscript(text, responseSeconds);
        } else {
            showPlaceholder("No speech was detected. Speak closer to the microphone and record again.");
        }

        setState("idle");
    } catch (error) {
        // fetch() throws a TypeError when the server can't be reached at all.
        if (error instanceof TypeError) {
            showError("The server couldn't be reached. Check if the app is running, then record again.");
        } else {
            showError(error.message);
        }
    }
}

function showTranscript(text, responseSeconds) {
    transcriptElement.textContent = text;
    transcriptElement.dataset.empty = "false";
    copyButton.disabled = false;

    const wordCount = text.split(/\s+/).filter(Boolean).length;
    metaWords.textContent = wordCount === 1 ? "1 word" : wordCount + " words";
    metaDuration.textContent = recordingSeconds.toFixed(1) + " s recorded";
    metaLatency.textContent = "Transcribed in " + responseSeconds.toFixed(1) + " s";
    metaList.hidden = false;
}

function showPlaceholder(message) {
    transcriptElement.textContent = message;
    transcriptElement.dataset.empty = "true";
    copyButton.disabled = true;
    metaList.hidden = true;
}

/**
 * Copies the transcript to the clipboard and briefly confirms on the button.
 */
async function copyTranscript() {
    try {
        await navigator.clipboard.writeText(transcriptElement.textContent);
        copyLabel.textContent = "Copied";
    } catch (error) {
        copyLabel.textContent = "Copy failed";
    }

    clearTimeout(copyResetId);
    copyResetId = setTimeout(() => {
        copyLabel.textContent = "Copy";
    }, 2000);
}

/* Live waveform  */

/**
 * Makes the waveform bars follow the microphone in real time.
 *
 * An AnalyserNode reads the frequency spectrum each animation frame.
 */
function startVisualiser(stream) {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) {
        return; // Bars stay static; recording still works.
    }

    audioContext = new AudioContextClass();
    audioContext.resume().catch(() => {});

    const analyser = audioContext.createAnalyser();
    analyser.fftSize = 256;
    analyser.smoothingTimeConstant = 0.75;
    audioContext.createMediaStreamSource(stream).connect(analyser);

    const frequencies = new Uint8Array(analyser.frequencyBinCount);
    const centreIndex = (bars.length - 1) / 2;

    const drawFrame = () => {
        analyser.getByteFrequencyData(frequencies);

        bars.forEach((bar, index) => {
            const distanceFromCentre = Math.abs(index - centreIndex);
            const firstBin = 2 + distanceFromCentre * 3;
            const loudness = (frequencies[firstBin] + frequencies[firstBin + 1] + frequencies[firstBin + 2]) / (3 * 255);

            // Quiet sits at 20% height; loud speech can reach 130% of the resting shape.
            const level = 0.2 + Math.min(loudness * 1.4, 1.1);
            bar.style.setProperty("--level", level.toFixed(2));
        });

        animationFrameId = requestAnimationFrame(drawFrame);
    };

    drawFrame();
}

/**
 * Stops the animation loop, closes the audio graph and restores the resting waveform.
 */
function stopVisualiser() {
    if (animationFrameId !== null) {
        cancelAnimationFrame(animationFrameId);
        animationFrameId = null;
    }

    if (audioContext) {
        audioContext.close().catch(() => {});
        audioContext = null;
    }

    bars.forEach((bar) => bar.style.removeProperty("--level"));
}

/* Start-up */

/**
 * Disables the control up front if the browser can't record at all.
 */
function checkBrowserSupport() {
    const supported = Boolean(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)
        && typeof MediaRecorder !== "undefined";

    if (!supported) {
        toggleButton.disabled = true;
        statusElement.dataset.state = "error";
        statusElement.textContent = "This browser can't record audio. Open the page in a current version of Chrome, Firefox, Edge or Safari.";
    }
}

toggleButton.addEventListener("click", handleToggleClick);
copyButton.addEventListener("click", copyTranscript);
checkBrowserSupport();