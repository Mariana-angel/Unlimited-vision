package com.example.unlimitedvision.audio

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Interruptor global del asistente de voz ("modo acompañante").
 * Cuando está apagado, la app no habla ni se pone a escuchar sola;
 * útil cuando quien usa el celular es un familiar o acompañante.
 * Se guarda en el celular, así que se recuerda al cerrar la app.
 */
object VoiceSettings {
    private const val PREFS = "unlimited_vision_prefs"
    private const val KEY = "key_voice_enabled"
    private const val KEY_RATE = "key_voice_rate"

    /** Velocidad de la voz: 0.5 (lenta) a 2.0 (rápida) */
    var speechRate by mutableFloatStateOf(1f)
        private set

    /** AudioAssistant lo usa para aplicar la nueva velocidad al instante */
    var onRateChange: ((Float) -> Unit)? = null

    fun setSpeechRate(context: Context, value: Float) {
        speechRate = value.coerceIn(0.5f, 2f)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY_RATE, speechRate).apply()
        onRateChange?.invoke(speechRate)
    }

    /** Compose observa este valor: los botones cambian de ícono solos */
    var enabled by mutableStateOf(true)
        private set

    /** MainActivity lo usa para callar la voz o volver a hablar al cambiar el modo */
    var onChange: ((Boolean) -> Unit)? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabled = prefs.getBoolean(KEY, true)
        speechRate = prefs.getFloat(KEY_RATE, 1f)
    }

    fun setEnabled(context: Context, value: Boolean) {
        if (enabled == value) return
        enabled = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply()
        onChange?.invoke(value)
    }

    fun toggle(context: Context) = setEnabled(context, !enabled)
}
