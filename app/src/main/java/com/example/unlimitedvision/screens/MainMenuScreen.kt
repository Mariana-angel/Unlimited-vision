package com.example.unlimitedvision.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unlimitedvision.audio.VoiceSettings
import com.example.unlimitedvision.components.AppBottomNavBar
import com.example.unlimitedvision.components.AppHeader
import com.example.unlimitedvision.components.rememberHaptic
import com.example.unlimitedvision.location.LocationHelper
import com.example.unlimitedvision.location.Ubicacion
import com.example.unlimitedvision.theme.CardBgTeal
import com.example.unlimitedvision.theme.DarkRedButton
import com.example.unlimitedvision.theme.DarkTealBg
import com.example.unlimitedvision.theme.LinkCyan
import com.example.unlimitedvision.theme.OrangeAction
import com.example.unlimitedvision.theme.TealHeader
import com.example.unlimitedvision.theme.UnlimitedVisionTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.unlimitedvision.data.AppConstants

private fun saludoSegunHora(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

@Composable
fun MainMenuScreen(
    onBackClick: () -> Unit,
    onNavigateToTransport: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onLogout: () -> Unit,
    onSpeak: (String) -> Unit,
    userName: String = "Usuario",
    onMicClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val prefs = remember { com.example.unlimitedvision.data.UserPreferences(context) }
    val contactoNombre = remember { prefs.getEmergencyContactName() }
    val contactoTelefono = remember { prefs.getEmergencyContactPhone() }

    val primerNombre = userName.trim().substringBefore(" ").ifBlank { "Usuario" }
    val saludo = remember { saludoSegunHora() }
    val fecha = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO")).format(Date())
            .replaceFirstChar { it.uppercase() }
    }

    var ubicacion by remember { mutableStateOf<Ubicacion?>(null) }
    var buscandoUbicacion by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    fun actualizarUbicacion() {
        buscandoUbicacion = true
        scope.launch {
            ubicacion = LocationHelper.obtenerUbicacion(context)
            buscandoUbicacion = false
        }
    }
    LaunchedEffect(Unit) { actualizarUbicacion() }

    val textoUbicacion = when {
        buscandoUbicacion -> "Buscando tu ubicación…"
        ubicacion?.direccion != null -> ubicacion!!.direccion!!
        ubicacion != null -> "Lat ${"%.4f".format(ubicacion!!.lat)}, Lng ${"%.4f".format(ubicacion!!.lng)}"
        !LocationHelper.tienePermiso(context) -> "Activa el permiso de ubicación"
        else -> "No pude obtener tu ubicación"
    }

    fun leerPantalla() {
        onSpeak(
            "$saludo, $primerNombre. Hoy es $fecha. Estás en: $textoUbicacion. " +
                "Opciones: tomar transporte, abrir cámara, hablar con el asistente y mis rutas. " +
                "Para emergencias, mantén presionado el botón rojo de SOS."
        )
    }

    fun activarSOS() {
        haptic()
        if (contactoTelefono != null) {
            onSpeak("Alerta SOS activada. Preparando un mensaje con tu ubicación para ${contactoNombre ?: "tu contacto de emergencia"}. Solo toca enviar.")
            LocationHelper.enviarSmsEmergencia(context, contactoTelefono, ubicacion)
        } else {
            onSpeak("Alerta SOS activada. Compartiendo tu ubicación. Configura un contacto de emergencia en tu perfil para avisarle directo.")
            LocationHelper.compartirUbicacion(context, ubicacion, "🆘 Necesito ayuda.")
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                showBack = false,
                onBackClick = onBackClick,
                onSpeakerClick = { leerPantalla() },
                onNotificationClick = { onSpeak("Notificación: tienes paradas del SITP cerca de tu ubicación.") }
            )
        },
        bottomBar = {
            AppBottomNavBar(
                selectedTab = "home",
                onHomeClick = { onSpeak("Ya estás en el inicio.") },
                onRoutesClick = onNavigateToTransport,
                onSpeakerClick = { leerPantalla() },
                onHelpClick = {
                    onSpeak("Ayuda. Toca el micrófono y di: transporte, cámara, ubicación o SOS. También puedes tocar las tarjetas grandes.")
                },
                onViajarClick = onNavigateToTransport
            )
        },
        containerColor = DarkTealBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // ---------- Saludo ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(TealHeader, Color(0xFF2F5A66))))
                    .clickable { haptic(); leerPantalla() }
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(fecha, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "$saludo, $primerNombre 👋",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                    // Avatar: abre el perfil
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DarkTealBg)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { haptic(); onSpeak("Abriendo tu perfil."); onNavigateToProfile() }
                            .semantics { contentDescription = "Abrir mi perfil" }
                    ) {
                        Text(primerNombre.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        icon = if (VoiceSettings.enabled) Icons.Default.RecordVoiceOver else Icons.Default.VoiceOverOff,
                        text = if (VoiceSettings.enabled) "Voz activa" else "Voz apagada",
                        ok = VoiceSettings.enabled
                    )
                    StatusChip(
                        icon = if (ubicacion != null) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        text = if (ubicacion != null) "GPS listo" else "Sin GPS",
                        ok = ubicacion != null
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---------- Ubicación ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBgTeal)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DarkTealBg)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeAction, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { haptic(); onSpeak("Estás en: $textoUbicacion") }
                    ) {
                        Text("Estás en", color = Color.LightGray, fontSize = 12.sp)
                        Text(textoUbicacion, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (buscandoUbicacion) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = LinkCyan, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { haptic(); actualizarUbicacion(); onSpeak("Actualizando ubicación.") }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar ubicación", tint = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MiniAction(Icons.Default.Share, "Compartir", Color(0xFF2E7D32), Modifier.weight(1f)) {
                        onSpeak("Compartiendo tu ubicación.")
                        LocationHelper.compartirUbicacion(context, ubicacion)
                    }
                    MiniAction(Icons.Default.Phone, "Llamar 123", Color(0xFF37474F), Modifier.weight(1f)) {
                        onSpeak("Abriendo la línea de emergencias 1 2 3.")
                        LocationHelper.marcar(context, AppConstants.LINEA_EMERGENCIA)
                    }
                    MiniAction(Icons.Default.CameraAlt, "Cámara", Color(0xFFAD1457), Modifier.weight(1f)) {
                        onNavigateToCamera()
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Accesos rápidos",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(10.dp))

            // ---------- Accesos rápidos 2x2 ----------
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                QuickTile(Icons.Default.DirectionsBus, "Tomar transporte", "TransMilenio y SITP", OrangeAction, Modifier.weight(1f)) {
                    onSpeak("Abriendo transporte.")
                    onNavigateToTransport()
                }
                QuickTile(Icons.Default.CameraAlt, "Abrir cámara", "Describe tu entorno", Color(0xFFAD1457), Modifier.weight(1f)) {
                    onSpeak("Abriendo cámara.")
                    onNavigateToCamera()
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                QuickTile(Icons.Default.Mic, "Hablar", "Dale una orden por voz", LinkCyan, Modifier.weight(1f)) {
                    onSpeak("Te escucho. Di transporte, cámara, ubicación o SOS.")
                    onMicClick()
                }
                QuickTile(Icons.Default.History, "Mis rutas", "Tus viajes recientes", Color(0xFF7E57C2), Modifier.weight(1f)) {
                    onSpeak("Abriendo tus rutas.")
                    onNavigateToTransport()
                }
            }

            Spacer(Modifier.height(26.dp))

            // ---------- SOS ----------
            SosButton(
                onTap = { haptic(); onSpeak("Botón de emergencia. Mantén presionado para activar el SOS.") },
                onLongPress = { activarSOS() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))

            // ---------- Cerrar sesión ----------
            OutlinedButton(
                onClick = { haptic(); showLogoutDialog = true; onSpeak("¿Seguro que quieres cerrar sesión?") },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFF8A80)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Cerrar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp)) // espacio para el botón flotante "viajar"
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = CardBgTeal,
            title = { Text("Cerrar sesión", color = Color.White, fontFamily = FontFamily.Serif) },
            text = { Text("¿Seguro que quieres salir de tu cuenta, $primerNombre?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onSpeak("Sesión cerrada. Hasta pronto.")
                    onLogout()
                }) { Text("Sí, salir", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false; onSpeak("Cancelado.") }) {
                    Text("Cancelar", color = LinkCyan)
                }
            }
        )
    }
}

@Composable
private fun StatusChip(icon: ImageVector, text: String, ok: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (ok) Color(0xFF69F0AE) else Color(0xFFFF8A80))
        )
        Spacer(Modifier.width(6.dp))
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniAction(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = rememberHaptic()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkTealBg)
            .clickable { haptic(); onClick() }
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = rememberHaptic()
    Column(
        modifier = modifier
            .heightIn(min = 128.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBgTeal)
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable { haptic(); onClick() }
            .semantics(mergeDescendants = true) {}
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.LightGray, fontSize = 12.sp)
    }
}

/** Botón SOS grande: un toque explica, mantener presionado lo activa (evita alertas por accidente) */
@Composable
private fun SosButton(onTap: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sos")
    val ring by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
            Box(
                Modifier
                    .size(120.dp)
                    .scale(ring)
                    .clip(CircleShape)
                    .background(DarkRedButton.copy(alpha = 0.3f))
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFE53935), DarkRedButton)))
                    .border(4.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .semantics { contentDescription = "Botón de emergencia SOS. Mantén presionado para activar." }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
                    }
            ) {
                Text("SOS", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
            }
        }
        Text(
            "Mantén presionado para pedir ayuda",
            color = Color.LightGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    UnlimitedVisionTheme {
        MainMenuScreen(
            onBackClick = {},
            onNavigateToTransport = {},
            onNavigateToCamera = {},
            onLogout = {},
            onSpeak = {},
            userName = "Pablo Leyton"
        )
    }
}
