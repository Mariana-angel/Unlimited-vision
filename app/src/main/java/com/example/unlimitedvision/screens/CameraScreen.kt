package com.example.unlimitedvision.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import androidx.camera.core.Preview as CameraCorePreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.unlimitedvision.components.VoiceToggleButton
import com.example.unlimitedvision.components.rememberHaptic
import com.example.unlimitedvision.theme.LinkCyan
import com.example.unlimitedvision.theme.OrangeAction
import com.example.unlimitedvision.theme.TealHeader
import com.example.unlimitedvision.theme.UnlimitedVisionTheme
import kotlinx.coroutines.delay

private data class Deteccion(val icono: ImageVector, val texto: String, val distancia: String?, val alerta: Boolean = false)

private enum class ModoCamara(val titulo: String, val icono: ImageVector, val ayuda: String) {
    EXPLORAR("Explorar", Icons.Default.Explore, "Te digo qué lugares y obstáculos tienes cerca."),
    TEXTO("Leer texto", Icons.Default.TextFields, "Apunta a un letrero o documento y te lo leo."),
    BILLETES("Billetes", Icons.Default.AttachMoney, "Muestra un billete o moneda y te digo su valor.")
}

/**
 * Detecciones de DEMOSTRACIÓN. Aquí se conectaría un modelo de visión real
 * (por ejemplo ML Kit para texto y objetos).
 */
private fun deteccionesDe(modo: ModoCamara, nombre: String): List<Deteccion> = when (modo) {
    ModoCamara.EXPLORAR -> listOf(
        Deteccion(Icons.Default.Warning, "$nombre, cuidado: hay un escalón al frente", "1 m", alerta = true),
        Deteccion(Icons.Default.DirectionsBus, "Parada del SITP a tu derecha", "7 m"),
        Deteccion(Icons.Default.Restaurant, "Restaurante a tu izquierda", "10 m"),
        Deteccion(Icons.AutoMirrored.Filled.DirectionsWalk, "Paso peatonal adelante", "15 m")
    )
    ModoCamara.TEXTO -> listOf(
        Deteccion(Icons.Default.Signpost, "Letrero: \"Calle 26 - Avenida El Dorado\"", null),
        Deteccion(Icons.Default.TextFields, "Aviso: \"Salida de emergencia\"", null)
    )
    ModoCamara.BILLETES -> listOf(
        Deteccion(Icons.Default.AttachMoney, "Billete de 20.000 pesos colombianos", null),
        Deteccion(Icons.Default.AttachMoney, "Moneda de 500 pesos", null)
    )
}

@Composable
fun CameraScreen(
    userName: String = "Usuario",
    onBackClick: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = rememberHaptic()
    val nombre = userName.trim().substringBefore(" ").ifBlank { "Usuario" }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (granted) onSpeak("Cámara activa.") else onSpeak("Necesito permiso de cámara para describir tu entorno.")
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var linterna by remember { mutableStateOf(false) }
    var cuadricula by remember { mutableStateOf(false) }
    var modo by remember { mutableStateOf(ModoCamara.EXPLORAR) }
    val detecciones = remember(modo, nombre) { deteccionesDe(modo, nombre) }
    var indice by remember { mutableIntStateOf(0) }

    val previewView = remember { PreviewView(context) }

    // ---------- Lectura de texto REAL con ML Kit ----------
    var textoDetectado by remember { mutableStateOf("") }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(Unit) { onDispose { recognizer.close() } }
    val leyendoTexto = remember { AtomicBoolean(false) }
    LaunchedEffect(modo) {
        leyendoTexto.set(modo == ModoCamara.TEXTO)
        if (modo != ModoCamara.TEXTO) textoDetectado = ""
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        else onSpeak("Cámara de asistencia activa, modo explorar. Toca el botón grande del centro para que te describa lo que tienes enfrente.")
    }

    // Vuelve a conectar la cámara cada vez que se cambia entre frontal y trasera
    // (antes el botón de cambiar cámara no hacía nada)
    LaunchedEffect(lensFacing, hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = CameraCorePreview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    crearAnalizadorTexto(recognizer, leyendoTexto) { texto -> textoDetectado = texto }
                )
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                linterna = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Las detecciones van "apareciendo" una por una, como si la cámara las encontrara
    LaunchedEffect(modo) {
        indice = 0
        while (true) {
            delay(3500)
            indice = (indice + 1) % detecciones.size
        }
    }

    fun describir() {
        haptic()
        val texto = when (modo) {
            ModoCamara.EXPLORAR -> "$nombre, esto es lo que veo: " + detecciones.joinToString(". ") { d -> d.texto + (d.distancia?.let { ", a $it" } ?: "") }
            ModoCamara.TEXTO -> if (textoDetectado.isBlank()) "No encuentro texto todavía. Acerca o aleja la cámara del letrero, despacio."
                else "El texto dice: $textoDetectado"
            ModoCamara.BILLETES -> "Dinero detectado. " + detecciones.joinToString(". ") { it.texto }
        }
        onSpeak(texto.replace(" m", " metros"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Icon(Icons.Default.NoPhotography, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Necesito permiso de cámara", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = TealHeader)
                ) { Text("Dar permiso") }
            }
        }

        // ---------- Cuadrícula (regla de tercios, ayuda a centrar) ----------
        if (cuadricula) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Color.White.copy(alpha = 0.35f)
                for (i in 1..2) {
                    drawLine(c, Offset(size.width * i / 3f, 0f), Offset(size.width * i / 3f, size.height), strokeWidth = 2f)
                    drawLine(c, Offset(0f, size.height * i / 3f), Offset(size.width, size.height * i / 3f), strokeWidth = 2f)
                }
            }
        }

        // ---------- Barra superior ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CircleIcon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { onBackClick() }
                Spacer(Modifier.weight(1f))
                VoiceToggleButton()
                Spacer(Modifier.width(10.dp))
                CircleIcon(Icons.AutoMirrored.Filled.VolumeUp, "Escuchar ayuda de la cámara") {
                    onSpeak("Modo ${modo.titulo}. ${modo.ayuda} Abajo puedes cambiar de modo, encender la linterna o cambiar de cámara.")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252))
                )
                Spacer(Modifier.width(8.dp))
                Text("Analizando · ${modo.titulo}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                val esReal = modo == ModoCamara.TEXTO
                Text(
                    if (esReal) "IA EN VIVO" else "DEMO",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (esReal) Color(0xFF69F0AE) else OrangeAction)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // ---------- Detección actual ----------
        AnimatedContent(
            targetState = if (modo == ModoCamara.TEXTO) {
                if (textoDetectado.isBlank()) Deteccion(Icons.Default.TextFields, "Apunta la cámara a un letrero o documento…", null)
                else Deteccion(Icons.Default.TextFields, "Dice: " + textoDetectado.take(140) + if (textoDetectado.length > 140) "…" else "", null)
            } else detecciones.getOrNull(indice) ?: detecciones.first(),
            transitionSpec = { (fadeIn() + slideInVertically { it / 3 }) togetherWith fadeOut() },
            label = "deteccion",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) { d ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (d.alerta) Color(0xE6B71C1C) else Color(0xD9102027))
                    .border(2.dp, if (d.alerta) Color(0xFFFF8A80) else LinkCyan, RoundedCornerShape(18.dp))
                    .clickable { haptic(); onSpeak(d.texto + (d.distancia?.let { ", a ${it.replace(" m", " metros")}" } ?: "")) }
                    .padding(16.dp)
            ) {
                Icon(d.icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f, fill = false)) {
                    Text(d.texto, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (d.distancia != null) Text("a ${d.distancia}", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                }
            }
        }

        // ---------- Panel inferior ----------
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            // Modos
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                ModoCamara.entries.forEach { m ->
                    val sel = m == modo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (sel) Color.White else Color.White.copy(alpha = 0.15f))
                            .clickable { haptic(); modo = m; onSpeak("Modo ${m.titulo}. ${m.ayuda}") }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(m.icono, contentDescription = null, tint = if (sel) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(m.titulo, color = if (sel) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Controles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircleIcon(if (linterna) Icons.Default.FlashOn else Icons.Default.FlashOff, if (linterna) "Apagar linterna" else "Encender linterna", activo = linterna) {
                        val cam = camera
                        if (cam?.cameraInfo?.hasFlashUnit() == true) {
                            linterna = !linterna
                            cam.cameraControl.enableTorch(linterna)
                            onSpeak(if (linterna) "Linterna encendida." else "Linterna apagada.")
                        } else {
                            onSpeak("Esta cámara no tiene linterna.")
                        }
                    }
                    Text("Linterna", color = Color.White, fontSize = 11.sp)
                }

                // Botón principal: describir
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(5.dp, LinkCyan, CircleShape)
                            .clickable { describir() }
                            .semantics { contentDescription = "Describir lo que veo" }
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.Black, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Describir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircleIcon(Icons.Default.Cameraswitch, "Cambiar de cámara") {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        onSpeak(if (lensFacing == CameraSelector.LENS_FACING_FRONT) "Cámara frontal." else "Cámara trasera.")
                    }
                    Text("Girar", color = Color.White, fontSize = 11.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircleIcon(if (cuadricula) Icons.Default.GridOn else Icons.Default.GridOff, if (cuadricula) "Quitar cuadrícula" else "Mostrar cuadrícula", activo = cuadricula) {
                        cuadricula = !cuadricula
                    }
                    Text("Guía", color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                modo.ayuda,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

/**
 * Analiza los fotogramas de la cámara (máximo uno cada 1,2 s para no gastar batería)
 * y devuelve el texto que encuentra ML Kit.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun crearAnalizadorTexto(
    recognizer: TextRecognizer,
    activo: AtomicBoolean,
    onTexto: (String) -> Unit
): ImageAnalysis.Analyzer {
    val ultimo = AtomicLong(0)
    return ImageAnalysis.Analyzer { proxy ->
        val ahora = System.currentTimeMillis()
        val media = proxy.image
        if (!activo.get() || media == null || ahora - ultimo.get() < 1200) {
            proxy.close()
            return@Analyzer
        }
        ultimo.set(ahora)
        val imagen = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        recognizer.process(imagen)
            .addOnSuccessListener { resultado ->
                val texto = resultado.text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
                if (texto.isNotEmpty()) onTexto(texto)
            }
            .addOnCompleteListener { proxy.close() }
    }
}

@Composable
private fun CircleIcon(icon: ImageVector, description: String, activo: Boolean = false, onClick: () -> Unit) {
    val haptic = rememberHaptic()
    IconButton(
        onClick = { haptic(); onClick() },
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (activo) OrangeAction else Color.Black.copy(alpha = 0.5f))
    ) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    UnlimitedVisionTheme {
        CameraScreen(userName = "Pablo", onBackClick = {}, onSpeak = {})
    }
}
