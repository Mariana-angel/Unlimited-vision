package com.example.unlimitedvision.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.ui.platform.LocalContext
import com.example.unlimitedvision.audio.VoiceSettings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unlimitedvision.theme.OrangeAction
import com.example.unlimitedvision.theme.TealHeader

/**
 * Botón para activar/desactivar el asistente de voz (modo acompañante).
 * Se puede poner en cualquier pantalla: el estado es global.
 */
@Composable
fun VoiceToggleButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val enabled = VoiceSettings.enabled
    IconButton(
        onClick = { VoiceSettings.toggle(context) },
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) Color(0xFF334A54) else Color(0xFF8B2626))
    ) {
        Icon(
            imageVector = if (enabled) Icons.Default.RecordVoiceOver else Icons.Default.VoiceOverOff,
            contentDescription = if (enabled) "Desactivar asistente de voz" else "Activar asistente de voz",
            tint = Color.White
        )
    }
}

/** Franja que avisa que la voz está apagada, con botón para volver a activarla */
@Composable
fun VoiceOffBanner() {
    val context = LocalContext.current
    if (!VoiceSettings.enabled) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF8B2626))
                .clickable { VoiceSettings.setEnabled(context, true) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.VoiceOverOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Modo acompañante: voz desactivada",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text("ACTIVAR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AppHeader(
    titleBanner: String? = null,
    showBack: Boolean = true,
    onBackClick: () -> Unit = {},
    onSpeakerClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TealHeader)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (titleBanner != null) {
            Text(
                text = titleBanner,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2C3E47))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VoiceToggleButton()

                IconButton(
                    onClick = onSpeakerClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334A54))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Asistente de voz",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334A54))
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color.White
                    )
                }
            }
        }
    }
    // Aviso visible cuando el asistente de voz está apagado
    VoiceOffBanner()
    }
}

@Composable
fun AppBottomNavBar(
    selectedTab: String,
    onHomeClick: () -> Unit,
    onRoutesClick: () -> Unit,
    onSpeakerClick: () -> Unit,
    onHelpClick: () -> Unit,
    onViajarClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom bar container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFFC7D0D3)),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onHomeClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Inicio",
                    tint = if (selectedTab == "home") Color.Black else Color.DarkGray
                )
                Text(
                    text = "Inicio",
                    fontSize = 11.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Rutas
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onRoutesClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "Rutas",
                    tint = if (selectedTab == "routes") Color.Black else Color.DarkGray
                )
                Text(
                    text = "Rutas",
                    fontSize = 11.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Speaker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSpeakerClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Voz",
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Ayuda
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onHelpClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Ayuda",
                    tint = Color.Black
                )
                Text(
                    text = "Ayuda",
                    fontSize = 11.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Floating Action Button "viajar" if supplied
        if (onViajarClick != null) {
            Box(
                modifier = Modifier
                    .offset(y = (-32).dp)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(OrangeAction)
                    .clickable { onViajarClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Viajar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "viajar",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
