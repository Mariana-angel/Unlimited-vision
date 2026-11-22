package com.example.unlimitedvision.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import java.util.Locale

class AudioAssistant(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    private var onSpeakDoneListener: (() -> Unit)? = null
    private val pendingWhenReady = mutableListOf<() -> Unit>()

    /** Sube el volumen del celular al máximo para que el asistente se escuche fuerte y claro */
    fun maximizarVolumen() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        val maxAcc = audioManager.getStreamMaxVolume(AudioManager.STREAM_ACCESSIBILITY)
                        audioManager.setStreamVolume(AudioManager.STREAM_ACCESSIBILITY, maxAcc, 0)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Ejecuta la acción cuando la voz (TTS) ya esté lista para hablar */
    fun whenReady(action: () -> Unit) {
        if (isReady) action() else pendingWhenReady.add(action)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.forLanguageTag("es-ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            tts?.setAudioAttributes(audioAttributes)
            tts?.setSpeechRate(VoiceSettings.speechRate)
            VoiceSettings.onRateChange = { rate -> tts?.setSpeechRate(rate) }
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    onSpeakDoneListener?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })

            isReady = true
            maximizarVolumen()
            pendingWhenReady.forEach { it() }
            pendingWhenReady.clear()
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        // Modo acompañante: no habla y tampoco encadena la escucha (onDone no se llama)
        if (!VoiceSettings.enabled) {
            onSpeakDoneListener = null
            return
        }
        onSpeakDoneListener = onDone

        // Eleva el volumen del sistema al máximo en cada voz
        maximizarVolumen()

        if (isReady && tts != null) {
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) // 100% de volumen relativo
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "UnlimitedVisionTTS_${System.currentTimeMillis()}")
        } else {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            onDone?.invoke()
        }
    }

    /** Corta lo que esté diciendo en este momento */
    fun stopSpeaking() {
        onSpeakDoneListener = null
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
