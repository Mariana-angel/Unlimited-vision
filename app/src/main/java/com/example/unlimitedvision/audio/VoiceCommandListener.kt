package com.example.unlimitedvision.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.Locale

class VoiceCommandListener(
    private val context: Context,
    private val onCommandDetected: (String) -> Unit,
    private val onErrorDetected: ((errorCode: Int) -> Unit)? = null,
    private val onPartialText: ((String) -> Unit)? = null
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Toast.makeText(context, "Reconocimiento de voz no disponible.", Toast.LENGTH_SHORT).show()
                return
            }

            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@VoiceCommandListener)
                }
            }

            // Do NOT set EXTRA_PROMPT, as EXTRA_PROMPT forces Android to draw a system modal dialog that dims/blacks out the screen
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CO")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-CO")
                // Antes esperaba 10 segundos de silencio para "terminar" de escuchar,
                // por eso parecía que no reaccionaba. Ahora termina a los ~2 segundos.
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
            Toast.makeText(context, "🎤 Escuchando...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0]
            Toast.makeText(context, "Reconocido: \"$spokenText\"", Toast.LENGTH_SHORT).show()
            onCommandDetected(spokenText)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onPartialText?.invoke(matches[0])
        }
    }

    override fun onError(error: Int) {
        // Avisa al asistente para que vuelva a preguntar (él limita los reintentos)
        android.util.Log.w("VoiceCommandListener", "Error de reconocimiento: $error")
        onErrorDetected?.invoke(error)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
