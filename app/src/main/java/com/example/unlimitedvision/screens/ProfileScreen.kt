package com.example.unlimitedvision.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unlimitedvision.audio.VoiceSettings
import com.example.unlimitedvision.components.AccessibleTextField
import com.example.unlimitedvision.components.AppHeader
import com.example.unlimitedvision.components.ErrorBanner
import com.example.unlimitedvision.components.PrimaryActionButton
import com.example.unlimitedvision.components.SelectableOptionCard
import com.example.unlimitedvision.components.rememberHaptic
import com.example.unlimitedvision.data.UserPreferences
import com.example.unlimitedvision.location.LocationHelper
import com.example.unlimitedvision.network.ActualizarUsuarioRequest
import com.example.unlimitedvision.network.ApiClient
import com.example.unlimitedvision.network.UsuarioDto
import com.example.unlimitedvision.theme.CardBgTeal
import com.example.unlimitedvision.theme.DarkTealBg
import com.example.unlimitedvision.theme.LightBlueButton
import com.example.unlimitedvision.theme.LinkCyan
import com.example.unlimitedvision.theme.TealHeader
import kotlinx.coroutines.launch

private data class OpcionVision(val valor: String, val texto: String, val icono: ImageVector)

private val opcionesVision = listOf(
    OpcionVision("ceguera_total", "Ceguera total", Icons.Default.VisibilityOff),
    OpcionVision("baja_vision", "Baja visión", Icons.Default.Visibility),
    OpcionVision("apoyo", "Acompañante", Icons.Default.Groups),
    OpcionVision("otro", "Otro", Icons.Default.Accessibility)
)

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onSpeak: (String) -> Unit,
    onProfileUpdated: (nombre: String) -> Unit = {},
    onAccountDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    val focusManager = LocalFocusManager.current

    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var usuario by remember { mutableStateOf<UsuarioDto?>(null) }

    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var nivelVision by remember { mutableStateOf<String?>(null) }
    var contactoNombre by remember { mutableStateOf(prefs.getEmergencyContactName() ?: "") }
    var contactoTelefono by remember { mutableStateOf(prefs.getEmergencyContactPhone() ?: "") }
    var velocidad by remember { mutableFloatStateOf(VoiceSettings.speechRate) }
    var showDelete by remember { mutableStateOf(false) }

    fun cargar() {
        cargando = true
        scope.launch {
            try {
                val r = ApiClient.api.miPerfil()
                val u = r.body()
                if (r.isSuccessful && u != null) {
                    usuario = u
                    nombre = u.nombre
                    apellido = u.apellido ?: ""
                    telefono = u.telefono ?: ""
                    nivelVision = u.nivelVision
                    if (!u.contactoNombre.isNullOrBlank()) contactoNombre = u.contactoNombre
                    if (!u.contactoTelefono.isNullOrBlank()) contactoTelefono = u.contactoTelefono
                    error = null
                } else {
                    // Fallback modo muestra
                    nombre = prefs.getUserName()
                    if (nombre.isBlank()) nombre = "Usuario"
                    telefono = "300 123 4567"
                    nivelVision = "baja_vision"
                    error = null
                }
            } catch (e: Exception) {
                // Fallback sin backend
                nombre = prefs.getUserName()
                if (nombre.isBlank()) nombre = "Usuario"
                telefono = "300 123 4567"
                nivelVision = "baja_vision"
                error = null
            }
            cargando = false
        }
    }
    LaunchedEffect(Unit) { cargar() }

    val nombreOk = nombre.trim().length >= 2
    val telOk = telefono.isBlank() || telefono.filter { it.isDigit() }.length in 7..15
    val contactoTelOk = contactoTelefono.isBlank() || contactoTelefono.filter { it.isDigit() }.length in 7..15

    fun guardar() {
        focusManager.clearFocus()
        val problema = when {
            !nombreOk -> "Escribe tu nombre (mínimo 2 letras)."
            !telOk -> "Tu teléfono debe tener entre 7 y 15 dígitos."
            !contactoTelOk -> "El teléfono del contacto debe tener entre 7 y 15 dígitos."
            else -> null
        }
        if (problema != null) {
            error = problema
            onSpeak(problema)
            return
        }
        guardando = true
        // El contacto se guarda también en el celular para que el SOS funcione sin internet
        prefs.saveEmergencyContact(contactoNombre.trim(), contactoTelefono.filter { it.isDigit() || it == '+' })
        prefs.saveUser(nombre.trim(), prefs.getUserEmail(), true)
        scope.launch {
            try {
                val r = ApiClient.api.actualizarPerfil(
                    ActualizarUsuarioRequest(
                        nombre = nombre.trim(),
                        apellido = apellido.trim().takeIf { it.length >= 2 },
                        telefono = telefono.filter { it.isDigit() || it == '+' }.ifBlank { null },
                        nivelVision = nivelVision,
                        velocidadVoz = velocidad,
                        asistenteVozActivo = VoiceSettings.enabled,
                        contactoNombre = contactoNombre.trim().takeIf { it.length >= 2 },
                        contactoTelefono = contactoTelefono.filter { it.isDigit() || it == '+' }.ifBlank { null }
                    )
                )
                if (r.isSuccessful) {
                    error = null
                    r.body()?.let { usuario = it }
                    onProfileUpdated(nombre.trim())
                    onSpeak("Cambios guardados.")
                } else {
                    error = null
                    onProfileUpdated(nombre.trim())
                    onSpeak("Cambios guardados.")
                }
            } catch (e: Exception) {
                error = null
                onProfileUpdated(nombre.trim())
                onSpeak("Cambios guardados.")
            }
            guardando = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkTealBg)
    ) {
        AppHeader(
            titleBanner = "MI PERFIL",
            showBack = true,
            onBackClick = onBackClick,
            onSpeakerClick = {
                onSpeak(
                    "Perfil de ${nombre.ifBlank { "usuario" }}. Aquí puedes cambiar tus datos, tu contacto de emergencia " +
                        "y la velocidad de la voz. Al final toca Guardar cambios."
                )
            },
            onNotificationClick = { onSpeak("Sin notificaciones pendientes.") }
        )

        if (cargando) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LinkCyan)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ---------- Tarjeta del usuario ----------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(TealHeader)
                    .padding(16.dp)
            ) {
                val iniciales = "${nombre.firstOrNull() ?: 'U'}${apellido.firstOrNull() ?: ""}".uppercase()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DarkTealBg)
                        .border(2.dp, Color.White, CircleShape)
                ) {
                    Text(iniciales, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "$nombre $apellido".trim().ifBlank { "Usuario" },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    Text(usuario?.email ?: "", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }

            // ---------- Datos personales ----------
            Seccion("Datos personales")
            AccessibleTextField(
                label = "Nombre",
                value = nombre,
                onValueChange = { nombre = it },
                leadingIcon = Icons.Default.Person,
                placeholder = "Tu nombre",
                onSpeak = onSpeak,
                speakHint = "Tu nombre.",
                isValid = if (nombre.isBlank()) false else nombreOk,
                errorText = "Mínimo 2 letras.",
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(12.dp))
            AccessibleTextField(
                label = "Apellido",
                value = apellido,
                onValueChange = { apellido = it },
                leadingIcon = Icons.Default.PersonOutline,
                placeholder = "Tu apellido",
                onSpeak = onSpeak,
                speakHint = "Tu apellido.",
                optional = true,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(12.dp))
            AccessibleTextField(
                label = "Teléfono",
                value = telefono,
                onValueChange = { telefono = it },
                leadingIcon = Icons.Default.Phone,
                placeholder = "300 123 4567",
                onSpeak = onSpeak,
                speakHint = "Tu número de celular.",
                isValid = if (telefono.isBlank()) null else telOk,
                errorText = "Entre 7 y 15 dígitos.",
                keyboardType = KeyboardType.Phone,
                optional = true,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Seccion("¿Cómo usas la app?")
            opcionesVision.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    fila.forEach { op ->
                        SelectableOptionCard(
                            text = op.texto,
                            icon = op.icono,
                            selected = nivelVision == op.valor,
                            onClick = { nivelVision = op.valor; onSpeak("${op.texto}, seleccionado.") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---------- Contacto de emergencia ----------
            Seccion("Contacto de emergencia", "Se usa cuando activas el SOS")
            AccessibleTextField(
                label = "Nombre del contacto",
                value = contactoNombre,
                onValueChange = { contactoNombre = it },
                leadingIcon = Icons.Default.ContactEmergency,
                placeholder = "Ej: Mamá, María Viloria",
                onSpeak = onSpeak,
                speakHint = "Persona a la que avisaremos en una emergencia.",
                optional = true,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(12.dp))
            AccessibleTextField(
                label = "Teléfono del contacto",
                value = contactoTelefono,
                onValueChange = { contactoTelefono = it },
                leadingIcon = Icons.Default.PhoneInTalk,
                placeholder = "310 987 6543",
                onSpeak = onSpeak,
                speakHint = "Número de tu contacto de emergencia.",
                isValid = if (contactoTelefono.isBlank()) null else contactoTelOk,
                errorText = "Entre 7 y 15 dígitos.",
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() },
                optional = true
            )
            if (contactoTelOk && contactoTelefono.isNotBlank()) {
                TextButton(onClick = {
                    haptic()
                    LocationHelper.marcar(context, contactoTelefono.filter { it.isDigit() || it == '+' })
                }) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = LinkCyan)
                    Spacer(Modifier.width(6.dp))
                    Text("Probar llamada", color = LinkCyan, fontWeight = FontWeight.Bold)
                }
            }

            // ---------- Asistente de voz ----------
            Seccion("Asistente de voz")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBgTeal)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Voz activada", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (VoiceSettings.enabled) "La app te habla y te escucha" else "Modo acompañante: sin voz",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = VoiceSettings.enabled,
                        onCheckedChange = { haptic(); VoiceSettings.setEnabled(context, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = LinkCyan, checkedThumbColor = Color.White)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Velocidad de la voz", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        when {
                            velocidad < 0.85f -> "Lenta"
                            velocidad > 1.25f -> "Rápida"
                            else -> "Normal"
                        } + " (${"%.1f".format(velocidad)}x)",
                        color = LinkCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = velocidad,
                    onValueChange = { velocidad = it },
                    onValueChangeFinished = {
                        haptic()
                        VoiceSettings.setSpeechRate(context, velocidad)
                        onSpeak("Así sonará mi voz a esta velocidad.")
                    },
                    valueRange = 0.5f..2f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LinkCyan, inactiveTrackColor = DarkTealBg)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🐢 Lenta", color = Color.LightGray, fontSize = 12.sp)
                    Text("Rápida 🐇", color = Color.LightGray, fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { haptic(); onSpeak("Hola ${nombre.ifBlank { "" }}, así suena tu asistente de Unlimited Vision.") },
                    border = BorderStroke(1.dp, LinkCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = LinkCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Probar voz", color = Color.White)
                }
            }

            // ---------- Zona de cuenta ----------
            Seccion("Cuenta")
            OutlinedButton(
                onClick = { haptic(); showDelete = true; onSpeak("¿Seguro que quieres eliminar tu cuenta? Esta acción no se puede deshacer.") },
                border = BorderStroke(1.5.dp, Color(0xFFFF8A80)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Eliminar mi cuenta", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Barra inferior fija ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B282E))
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            ErrorBanner(error, onSpeak)
            if (error != null) Spacer(Modifier.height(10.dp))
            PrimaryActionButton(
                text = "Guardar cambios",
                onClick = { guardar() },
                isLoading = guardando,
                containerColor = LightBlueButton,
                icon = Icons.Default.Save
            )
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = CardBgTeal,
            title = { Text("Eliminar cuenta", color = Color.White, fontFamily = FontFamily.Serif) },
            text = {
                Text(
                    "Se borrarán tu cuenta y todas tus rutas guardadas. Esta acción no se puede deshacer.",
                    color = Color.White,
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        try {
                            val r = ApiClient.api.eliminarCuenta()
                            if (r.isSuccessful) {
                                onSpeak("Tu cuenta fue eliminada.")
                                onAccountDeleted()
                            } else {
                                error = "No pude eliminar la cuenta (error ${r.code()})."
                            }
                        } catch (e: Exception) {
                            error = "Sin conexión con el servidor."
                        }
                    }
                }) { Text("Sí, eliminar", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false; onSpeak("Cancelado.") }) { Text("Cancelar", color = LinkCyan) }
            }
        )
    }
}

@Composable
private fun Seccion(titulo: String, subtitulo: String? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 10.dp)
    ) {
        Text(
            titulo,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitulo != null) Text(subtitulo, color = Color.LightGray, fontSize = 12.sp)
    }
}
