package com.example.unlimitedvision.network

import com.google.gson.JsonParser
import retrofit2.Response
import java.io.IOException

/** Resultado simple para que las pantallas sepan si funcionó o qué decirle al usuario */
sealed class ApiResult<out T> {
    data class Exito<T>(val data: T) : ApiResult<T>()
    data class Error(val mensaje: String, val codigo: Int? = null) : ApiResult<Nothing>()
}

class AuthRepository(private val api: UnlimitedVisionApi = ApiClient.api) {

    /** Permite ingresar y registrarse sin necesidad de tener el backend encendido */
    var modoOfflineDemo: Boolean = true

    suspend fun registro(form: RegistroFormulario): ApiResult<AuthResponse> =
        registro(form.nombreCompleto, form.email, form.password, form.telefono, form.nivelVision)

    suspend fun registro(
        nombreCompleto: String,
        email: String,
        password: String,
        telefono: String? = null,
        nivelVision: String? = null
    ): ApiResult<AuthResponse> {
        val partes = nombreCompleto.trim().split(Regex("\\s+"), limit = 2)
        val nombre = partes[0].ifBlank { "Usuario" }
        val apellido = partes.getOrNull(1)?.takeIf { it.length >= 2 }
        val mail = email.trim().lowercase()

        val body = RegistroRequest(
            nombre = nombre,
            apellido = apellido,
            email = mail,
            password = password,
            telefono = telefono,
            nivelVision = nivelVision
        )

        return ejecutar(
            fallbackDemo = {
                AuthResponse(
                    accessToken = "mock_token_demo_${System.currentTimeMillis()}",
                    usuario = UsuarioDto(
                        id = 100,
                        nombre = nombre,
                        apellido = apellido,
                        email = mail,
                        telefono = telefono,
                        nivelVision = nivelVision ?: "baja_vision",
                        velocidadVoz = 1.0f,
                        asistenteVozActivo = true
                    )
                )
            }
        ) { api.registro(body) }
    }

    suspend fun login(email: String, password: String): ApiResult<AuthResponse> {
        val mail = email.trim().lowercase()
        val nombreExtraido = if (mail.contains("@")) {
            mail.substringBefore("@")
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                .ifBlank { "Usuario" }
        } else {
            "Usuario"
        }

        return ejecutar(
            fallbackDemo = {
                AuthResponse(
                    accessToken = "mock_token_demo_${System.currentTimeMillis()}",
                    usuario = UsuarioDto(
                        id = 100,
                        nombre = nombreExtraido,
                        apellido = null,
                        email = mail,
                        telefono = "300 123 4567",
                        nivelVision = "baja_vision",
                        velocidadVoz = 1.0f,
                        asistenteVozActivo = true
                    )
                )
            }
        ) { api.login(LoginRequest(mail, password)) }
    }

    private suspend fun <T> ejecutar(
        fallbackDemo: (() -> T)? = null,
        llamada: suspend () -> Response<T>
    ): ApiResult<T> {
        return try {
            val respuesta = llamada()
            val cuerpo = respuesta.body()
            if (respuesta.isSuccessful && cuerpo != null) {
                ApiResult.Exito(cuerpo)
            } else {
                if (fallbackDemo != null) {
                    ApiResult.Exito(fallbackDemo())
                } else {
                    ApiResult.Error(leerMensajeError(respuesta), respuesta.code())
                }
            }
        } catch (e: IOException) {
            if (fallbackDemo != null) {
                ApiResult.Exito(fallbackDemo())
            } else {
                ApiResult.Error("No me pude conectar con el servidor.")
            }
        } catch (e: Exception) {
            if (fallbackDemo != null) {
                ApiResult.Exito(fallbackDemo())
            } else {
                ApiResult.Error("Ocurrió un error inesperado.")
            }
        }
    }

    /** NestJS responde { "message": "..." } o { "message": ["...", "..."] } */
    private fun leerMensajeError(respuesta: Response<*>): String {
        return try {
            val json = JsonParser.parseString(respuesta.errorBody()?.string() ?: "").asJsonObject
            val mensaje = json.get("message")
            when {
                mensaje == null -> "Error ${respuesta.code()}"
                mensaje.isJsonArray -> mensaje.asJsonArray.joinToString(". ") { it.asString }
                else -> mensaje.asString
            }
        } catch (e: Exception) {
            "Error ${respuesta.code()}"
        }
    }
}
