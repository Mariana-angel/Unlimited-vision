package com.example.unlimitedvision.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unlimitedvision.components.OlivePillButton
import com.example.unlimitedvision.components.VoiceOffBanner
import com.example.unlimitedvision.components.VoiceToggleButton
import com.example.unlimitedvision.components.PulsingMicButton
import com.example.unlimitedvision.components.rememberHaptic
import com.example.unlimitedvision.theme.CardBgTeal
import com.example.unlimitedvision.theme.DarkTealBg
import com.example.unlimitedvision.theme.LinkCyan
import com.example.unlimitedvision.theme.TealHeader
import com.example.unlimitedvision.theme.UnlimitedVisionTheme

private data class Feature(val icon: ImageVector, val title: String, val description: String)

private val features = listOf(
    Feature(Icons.Default.RecordVoiceOver, "Asistente de voz", "Controla toda la app hablando"),
    Feature(Icons.Default.DirectionsBus, "Transporte", "Rutas y paradas del SITP guiadas"),
    Feature(Icons.Default.PhotoCamera, "Cámara", "Te describe lo que tienes enfrente"),
    Feature(Icons.Default.Warning, "SOS", "Comparte tu ubicación en emergencias")
)

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onSpeak: (String) -> Unit,
    onMicClick: () -> Unit = {}
) {
    val haptic = rememberHaptic()

    // Animación suave del logo (el "ojo" respira)
    val transition = rememberInfiniteTransition(label = "logo")
    val logoScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkTealBg)
            .verticalScroll(rememberScrollState())
    ) {
        // ---------- Encabezado con degradado ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(Brush.verticalGradient(listOf(TealHeader, Color(0xFF3A6E7B))))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(
                    onClick = {
                        haptic()
                        onSpeak(
                            "Pantalla de bienvenida de Unlimited Vision. Tienes dos botones grandes: " +
                                "Ingresar, si ya tienes cuenta, y Crear cuenta, si eres nuevo. " +
                                "Abajo está el botón del micrófono para hablar con el asistente."
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334A54))
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar descripción de la pantalla", tint = Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VoiceToggleButton(Modifier.size(48.dp))
                IconButton(
                    onClick = {
                        haptic()
                        onSpeak(
                            "Ayuda. Puedes tocar los botones o usar la voz. Di iniciar sesión, o di registrarme. " +
                                "En cualquier momento puedes decir cancelar para volver al inicio."
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334A54))
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Ayuda", tint = Color.White)
                }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, bottom = 28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(logoScale)
                        .clip(CircleShape)
                        .background(DarkTealBg)
                        .border(3.dp, Color(0xFFD4E3E5), CircleShape)
                        .semantics { contentDescription = "Logo de Unlimited Vision" }
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFD4E3E5), modifier = Modifier.size(58.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "UNLIMITED VISION",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFFD4E3E5),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tu ciudad, sin límites. Muévete con confianza.",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }

        VoiceOffBanner()

        // ---------- Funciones de la app ----------
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            Text(
                "¿Qué puedes hacer?",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(12.dp))
            features.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    fila.forEach { f ->
                        FeatureCard(f, onSpeak, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ---------- Botones principales ----------
            OlivePillButton("Ingresar", Icons.AutoMirrored.Filled.Login, onNavigateToLogin)
            Spacer(Modifier.height(14.dp))
            OlivePillButton("Crear cuenta", Icons.Default.PersonAdd, onNavigateToRegister)

            Spacer(Modifier.height(26.dp))

            // ---------- Asistente de voz ----------
            PulsingMicButton(
                onClick = {
                    onSpeak("Te escucho. Di iniciar sesión o registrarme.")
                    onMicClick()
                },
                label = "Toca para hablar con el asistente",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(18.dp))
            Text(
                "Consejo: toca cualquier tarjeta o el ícono 🔊 para escucharla en voz alta.",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Versión 1.0 · Hecho en Bogotá 🇨🇴",
                color = Color.Gray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature, onSpeak: (String) -> Unit, modifier: Modifier = Modifier) {
    val haptic = rememberHaptic()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBgTeal)
            .clickable { haptic(); onSpeak("${feature.title}. ${feature.description}.") }
            .semantics(mergeDescendants = true) {}
            .padding(14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(DarkTealBg)
        ) {
            Icon(feature.icon, contentDescription = null, tint = LinkCyan, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(feature.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(feature.description, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    UnlimitedVisionTheme {
        WelcomeScreen(onNavigateToLogin = {}, onNavigateToRegister = {}, onSpeak = {})
    }
}
