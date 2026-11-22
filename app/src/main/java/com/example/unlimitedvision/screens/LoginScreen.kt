package com.example.unlimitedvision.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unlimitedvision.components.AccessibleTextField
import com.example.unlimitedvision.components.AppHeader
import com.example.unlimitedvision.components.ErrorBanner
import com.example.unlimitedvision.components.OrDivider
import com.example.unlimitedvision.components.PrimaryActionButton
import com.example.unlimitedvision.components.SecondaryActionButton
import com.example.unlimitedvision.components.SuccessBanner
import com.example.unlimitedvision.components.rememberHaptic
import com.example.unlimitedvision.theme.CardBgTeal
import com.example.unlimitedvision.theme.DarkTealBg
import com.example.unlimitedvision.theme.LinkCyan
import com.example.unlimitedvision.theme.TealHeader
import com.example.unlimitedvision.theme.UnlimitedVisionTheme

@Composable
fun LoginScreen(
    emailState: String = "",
    passwordState: String = "",
    onBackClick: () -> Unit,
    // Envía los datos al backend; onResult(null) = éxito, onResult("mensaje") = error
    onLoginSubmit: (email: String, password: String, remember: Boolean, onResult: (String?) -> Unit) -> Unit,
    onNavigateToRegister: () -> Unit,
    onSpeak: (String) -> Unit,
    onVoiceLogin: () -> Unit = {}
) {
    var email by remember { mutableStateOf(emailState) }
    var password by remember { mutableStateOf(passwordState) }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    // Solo mostramos errores de un campo después de que el usuario escribió o intentó enviar
    var intentoEnviar by remember { mutableStateOf(false) }

    // Lo que dicta el asistente de voz llena los campos
    LaunchedEffect(emailState) { if (emailState.isNotBlank()) email = emailState }
    LaunchedEffect(passwordState) { if (passwordState.isNotBlank()) password = passwordState }

    val focusManager = LocalFocusManager.current
    val haptic = rememberHaptic()

    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val isPasswordValid = password.length >= 6

    fun handleLogin() {
        intentoEnviar = true
        focusManager.clearFocus()
        when {
            email.isBlank() || password.isBlank() -> {
                errorMessage = "Por favor escribe tu correo y tu contraseña."
                onSpeak("Por favor escribe tu correo electrónico y tu contraseña.")
            }
            !isEmailValid -> {
                errorMessage = "El correo no tiene un formato válido. Ejemplo: nombre@correo.com"
                onSpeak("El formato del correo electrónico no es válido.")
            }
            !isPasswordValid -> {
                errorMessage = "La contraseña debe tener al menos 6 caracteres."
                onSpeak("La contraseña debe tener al menos 6 caracteres.")
            }
            else -> {
                errorMessage = null
                isLoading = true
                onSpeak("Verificando tus datos.")
                onLoginSubmit(email.trim(), password, rememberMe) { error ->
                    if (error != null) {
                        isLoading = false
                        errorMessage = error
                        onSpeak(error)
                    } else {
                        errorMessage = null
                        successMessage = "¡Inicio de sesión exitoso!"
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkTealBg)
    ) {
        AppHeader(
            titleBanner = "INICIO DE SESIÓN",
            showBack = true,
            onBackClick = onBackClick,
            onSpeakerClick = {
                onSpeak(
                    "Pantalla de inicio de sesión. Escribe tu correo y tu contraseña y toca Iniciar sesión. " +
                        "También puedes tocar Ingresar con voz y el asistente te guiará."
                )
            },
            onNotificationClick = { onSpeak("Sin notificaciones pendientes.") }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(CardBgTeal)
                    .border(2.dp, TealHeader, CircleShape)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFD4E3E5), modifier = Modifier.size(48.dp))
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "¡Hola de nuevo!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color.White,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "NOS ALEGRA VERTE OTRA VEZ",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = Color.LightGray
            )

            Spacer(Modifier.height(24.dp))

            AccessibleTextField(
                label = "Correo electrónico",
                value = email,
                onValueChange = { email = it; errorMessage = null },
                leadingIcon = Icons.Default.Email,
                placeholder = "ejemplo@correo.com",
                onSpeak = onSpeak,
                speakHint = "Escribe el correo con el que te registraste.",
                isValid = if (email.isBlank() && !intentoEnviar) null else isEmailValid,
                errorText = "Revisa el correo, falta la @ o el dominio.",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
            )

            Spacer(Modifier.height(16.dp))

            AccessibleTextField(
                label = "Contraseña",
                value = password,
                onValueChange = { password = it; errorMessage = null },
                leadingIcon = Icons.Default.Lock,
                placeholder = "Tu contraseña",
                onSpeak = onSpeak,
                speakHint = "Escribe tu contraseña. Mínimo seis caracteres.",
                isValid = if (!intentoEnviar) null else isPasswordValid,
                errorText = "Mínimo 6 caracteres.",
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = { handleLogin() },
                isPassword = true
            )

            // Recordarme + olvidé la contraseña
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { haptic(); rememberMe = !rememberMe }
                        .padding(vertical = 4.dp)
                ) {
                    Switch(
                        checked = rememberMe,
                        onCheckedChange = { haptic(); rememberMe = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LinkCyan,
                            uncheckedTrackColor = Color(0xFF2C3E47)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Mantener sesión", color = Color.White, fontSize = 14.sp)
                }

                TextButton(onClick = {
                    haptic()
                    showForgotDialog = true
                    onSpeak("¿Olvidaste tu contraseña? Te mostraré las opciones para recuperarla.")
                }) {
                    Text("¿Olvidaste la contraseña?", color = LinkCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            ErrorBanner(errorMessage, onSpeak)
            SuccessBanner(successMessage, onSpeak)

            Spacer(Modifier.height(20.dp))

            PrimaryActionButton(
                text = "Iniciar sesión",
                onClick = { handleLogin() },
                isLoading = isLoading,
                icon = Icons.AutoMirrored.Filled.Login
            )

            OrDivider()

            SecondaryActionButton(
                text = "Ingresar con voz",
                icon = Icons.Default.Mic,
                onClick = {
                    errorMessage = null
                    onVoiceLogin()
                }
            )

            Spacer(Modifier.height(22.dp))

            // Ir a registro
            Text(
                text = buildAnnotatedString {
                    append("¿No tienes una cuenta? ")
                    withStyle(SpanStyle(color = LinkCyan, fontWeight = FontWeight.Bold)) { append("Regístrate aquí") }
                },
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { haptic(); onNavigateToRegister() }
                    .padding(12.dp)
            )

            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = CardBgTeal,
            title = { Text("Recuperar contraseña", color = Color.White, fontFamily = FontFamily.Serif) },
            text = {
                Text(
                    "Pronto podrás recuperar tu contraseña desde la app. Por ahora, pídele a tu acompañante " +
                        "o al soporte de Unlimited Vision que te ayude a restablecerla.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Entendido", color = LinkCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    UnlimitedVisionTheme {
        LoginScreen(
            onBackClick = {},
            onLoginSubmit = { _, _, _, _ -> },
            onNavigateToRegister = {},
            onSpeak = {}
        )
    }
}
