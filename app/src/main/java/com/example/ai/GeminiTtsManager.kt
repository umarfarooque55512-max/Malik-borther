package com.example.ai

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiTtsManager"
private const val TTS_MODEL_NAME = "gemini-3.8-flash-tts"
private const val SAMPLE_RATE_HZ = 24_000

sealed interface TtsState {
    data object Idle : TtsState
    data class Generating(val label: String) : TtsState
    data class Playing(val label: String) : TtsState
    data class Error(val message: String) : TtsState
}

class GeminiTtsManager {

    companion object {
        val availableVoices = listOf("Kore", "Puck", "Zephyr", "Fenrir", "Sulafat", "Schedar")
    }

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private val modelCache = mutableMapOf<String, GenerativeModel>()
    private var activeJob: Job? = null
    private var activeTrack: AudioTrack? = null

    @OptIn(PublicPreviewAPI::class)
    private fun getModelForVoice(voiceName: String): GenerativeModel {
        return modelCache.getOrPut(voiceName) {
            val config = generationConfig {
                responseModalities = listOf(ResponseModality.AUDIO)
                speechConfig = SpeechConfig(voice = Voice(voiceName))
            }
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = TTS_MODEL_NAME,
                generationConfig = config
            )
        }
    }

    fun speak(
        scope: CoroutineScope,
        label: String,
        spokenText: String,
        voiceName: String,
        isOfflineMode: Boolean
    ) {
        stop()

        if (spokenText.isBlank()) {
            _ttsState.value = TtsState.Error("Nothing to read aloud.")
            return
        }

        if (isOfflineMode) {
            _ttsState.value = TtsState.Error(
                "Offline Mode is enabled. Turn off Offline Mode to synthesize speech with Gemini TTS."
            )
            return
        }

        activeJob = scope.launch {
            _ttsState.value = TtsState.Generating(label)
            try {
                val prompt = "Read this story naturally and expressively in the same language as the text:\n$spokenText"
                val response = withTimeout(60_000L) {
                    getModelForVoice(voiceName).generateContent(prompt)
                }

                val inlineParts = response.candidates
                    .firstOrNull()
                    ?.content
                    ?.parts
                    ?.filterIsInstance<InlineDataPart>()
                    .orEmpty()

                if (inlineParts.isEmpty()) {
                    _ttsState.value = TtsState.Error("TTS model returned no audio data.")
                    return@launch
                }

                val totalBytes = inlineParts.sumOf { it.inlineData.size }
                val pcmData = ByteArray(totalBytes)
                var offset = 0
                for (part in inlineParts) {
                    part.inlineData.copyInto(pcmData, offset)
                    offset += part.inlineData.size
                }

                _ttsState.value = TtsState.Playing(label)
                playPcmAudio(pcmData)
                _ttsState.value = TtsState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini TTS timed out", e)
                _ttsState.value = TtsState.Error("Speech generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini TTS failed", e)
                _ttsState.value = TtsState.Error(
                    e.localizedMessage ?: "Failed to generate speech via Gemini TTS."
                )
            }
        }
    }

    private suspend fun playPcmAudio(pcmBytes: ByteArray) = withContext(Dispatchers.IO) {
        if (pcmBytes.isEmpty()) return@withContext

        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        activeTrack = audioTrack
        try {
            audioTrack.play()
            var written = 0
            val chunkSize = minBuffer
            while (written < pcmBytes.size) {
                val count = minOf(chunkSize, pcmBytes.size - written)
                val res = audioTrack.write(pcmBytes, written, count)
                if (res <= 0) break
                written += res
            }
            audioTrack.stop()
        } finally {
            try {
                audioTrack.release()
            } catch (_: Exception) {
            }
            if (activeTrack === audioTrack) {
                activeTrack = null
            }
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
        try {
            activeTrack?.pause()
            activeTrack?.flush()
            activeTrack?.release()
        } catch (_: Exception) {
        }
        activeTrack = null
        if (_ttsState.value !is TtsState.Error) {
            _ttsState.value = TtsState.Idle
        }
    }

    fun dismissError() {
        if (_ttsState.value is TtsState.Error) {
            _ttsState.value = TtsState.Idle
        }
    }
}
