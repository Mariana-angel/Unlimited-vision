package com.example.unlimitedvision.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints del backend (los mismos que se ven en Swagger) */
interface UnlimitedVisionApi {

    @POST("auth/registro")
    suspend fun registro(@Body body: RegistroRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("usuarios/me")
    suspend fun miPerfil(): Response<UsuarioDto>

    @PATCH("usuarios/me")
    suspend fun actualizarPerfil(@Body body: ActualizarUsuarioRequest): Response<UsuarioDto>

    @DELETE("usuarios/me")
    suspend fun eliminarCuenta(): Response<Unit>

    @POST("rutas")
    suspend fun crearRuta(@Body body: CrearRutaRequest): Response<RutaDto>

    @GET("rutas")
    suspend fun listarRutas(@Query("favoritas") favoritas: Boolean? = null): Response<List<RutaDto>>

    @PATCH("rutas/{id}")
    suspend fun actualizarRuta(@Path("id") id: Int, @Body body: ActualizarRutaRequest): Response<RutaDto>

    @DELETE("rutas/{id}")
    suspend fun eliminarRuta(@Path("id") id: Int): Response<Unit>
}
