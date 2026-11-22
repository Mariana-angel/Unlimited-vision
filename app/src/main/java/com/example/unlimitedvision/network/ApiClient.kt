package com.example.unlimitedvision.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /**
     * Dirección del backend:
     *  - Producción en internet: https://tu-backend.onrender.com/ o https://api.tudominio.com/
     *  - Túnel de prueba (ngrok / localtunnel): https://xxxx.ngrok-free.app/
     *  - Emulador de Android Studio: http://10.0.2.2:3000/
     *  - Celular físico por Wi-Fi o USB: http://<IP_PC>:3000/ o http://localhost:3000/
     */
    val BASE_URL: String by lazy {
        val configuredHost = com.example.unlimitedvision.BuildConfig.BACKEND_HOST.trim()
        when {
            // URL completa configurada en local.properties (ej: https://unlimitedvision-api.onrender.com)
            configuredHost.startsWith("http://") || configuredHost.startsWith("https://") -> {
                if (configuredHost.endsWith("/")) configuredHost else "$configuredHost/"
            }
            // Solo IP o host configurado (ej: 192.168.1.15 o api.midominio.com)
            configuredHost.isNotBlank() -> "http://$configuredHost:3000/"
            // Emulador de Android Studio: 10.0.2.2 es el "localhost" del PC
            esEmulador() -> "http://10.0.2.2:3000/"
            // Celular por cable USB (adb reverse tcp:3000 tcp:3000)
            else -> "http://localhost:3000/"
        }
    }

    private fun esEmulador(): Boolean {
        val b = android.os.Build.FINGERPRINT + android.os.Build.MODEL + android.os.Build.PRODUCT + android.os.Build.HARDWARE
        return b.contains("generic") || b.contains("emulator") || b.contains("sdk_gphone") ||
            b.contains("goldfish") || b.contains("ranchu") || b.contains("Android SDK built for")
    }

    /** Token JWT de la sesión actual; se envía solo en cada petición */
    @Volatile
    var token: String? = null

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().apply {
            token?.let { header("Authorization", "Bearer $it") }
        }.build()
        chain.proceed(request)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // ver peticiones en Logcat (filtro: okhttp)
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: UnlimitedVisionApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UnlimitedVisionApi::class.java)
    }
}
