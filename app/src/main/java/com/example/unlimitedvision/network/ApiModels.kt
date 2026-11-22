package com.example.unlimitedvision.network

// ---------- Lo que llena el usuario en la pantalla de registro ----------

data class RegistroFormulario(
    val nombreCompleto: String,
    val email: String,
    val password: String,
    val telefono: String? = null,
    val nivelVision: String? = null
)

// ---------- Lo que se ENVÍA al backend ----------

data class RegistroRequest(
    val nombre: String,
    val apellido: String? = null,
    val email: String,
    val password: String,
    val telefono: String? = null,
    val nivelVision: String? = null // ceguera_total | baja_vision | apoyo | otro
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class CrearRutaRequest(
    val origen: String,
    val destino: String,
    val origenLat: Double? = null,
    val origenLng: Double? = null,
    val destinoLat: Double? = null,
    val destinoLng: Double? = null,
    val medioTransporte: String? = null, // caminando | bus | transmilenio | taxi | bicicleta | otro
    val distanciaMetros: Int? = null,
    val duracionMinutos: Int? = null,
    val favorita: Boolean? = null
)

// ---------- Lo que RESPONDE el backend ----------

data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val apellido: String?,
    val email: String,
    val telefono: String?,
    val nivelVision: String?,
    val velocidadVoz: Float?,
    val asistenteVozActivo: Boolean?,
    val contactoNombre: String? = null,
    val contactoTelefono: String? = null
)

/** PATCH /usuarios/me: solo se envían los campos que no son null */
data class ActualizarUsuarioRequest(
    val nombre: String? = null,
    val apellido: String? = null,
    val telefono: String? = null,
    val nivelVision: String? = null,
    val velocidadVoz: Float? = null,
    val asistenteVozActivo: Boolean? = null,
    val contactoNombre: String? = null,
    val contactoTelefono: String? = null
)

/** PATCH /rutas/:id */
data class ActualizarRutaRequest(
    val favorita: Boolean? = null,
    val destino: String? = null,
    val medioTransporte: String? = null
)

data class AuthResponse(
    val accessToken: String,
    val usuario: UsuarioDto
)

data class RutaDto(
    val id: Int,
    val origen: String,
    val destino: String,
    val origenLat: Double?,
    val origenLng: Double?,
    val destinoLat: Double?,
    val destinoLng: Double?,
    val medioTransporte: String,
    val distanciaMetros: Int?,
    val duracionMinutos: Int?,
    val favorita: Boolean,
    val creadoEn: String?
)
