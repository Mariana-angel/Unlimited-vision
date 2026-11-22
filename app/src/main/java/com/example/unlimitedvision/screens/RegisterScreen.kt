package com.example.unlimitedvision.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.launch
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
import com.example.unlimitedvision.components.PasswordStrengthMeter
import com.example.unlimitedvision.components.PrimaryActionButton
import com.example.unlimitedvision.components.RequirementRow
import com.example.unlimitedvision.components.SecondaryActionButton
import com.example.unlimitedvision.components.SelectableOptionCard
import com.example.unlimitedvision.components.SuccessBanner
import com.example.unlimitedvision.components.rememberHaptic
import com.example.unlimitedvision.network.RegistroFormulario
import com.example.unlimitedvision.theme.DarkTealBg
import com.example.unlimitedvision.theme.LightBlueButton
import com.example.unlimitedvision.theme.LinkCyan
import com.example.unlimitedvision.theme.TealHeader
import com.example.unlimitedvision.theme.UnlimitedVisionTheme

private data class NivelOpcion(val valor: String, val texto: String, val icono: androidx.compose.ui.graphics.vector.ImageVector)

private val nivelesVision = listOf(
    NivelOpcion("ceguera_total", "Ceguera total", Icons.Default.VisibilityOff),
    NivelOpcion("baja_vision", "Baja visión", Icons.Default.Visibility),
    NivelOpcion("apoyo", "Soy acompañante", Icons.Default.Groups),
    NivelOpcion("otro", "Otro", Icons.Default.Accessibility)
)

@Composable
fun RegisterScreen(
    nombreState: String = "",
    emailState: String = "",
    passwordState: String = "",
    onBackClick: () -> Unit,
    // Envía los datos al backend; onResult(null) = éxito, onResult("mensaje") = error
    onRegisterSubmit: (form: RegistroFormulario, onResult: (String?) -> Unit) -> Unit,
    onSpeak: (String) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onVoiceRegister: () -> Unit = {}
) {
    var nombre by remember { mutableStateOf(nombreState) }
    var email by remember { mutableStateOf(emailState) }
    var telefono by remember { mutableStateOf("") }
    var nivelVision by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf(passwordState) }
    var confirmPassword by remember { mutableStateOf(passwordState) }
    var aceptaTerminos by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var intentoEnviar by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    // Lo que dicta el asistente de voz llena los campos
    LaunchedEffect(nombreState) { if (nombreState.isNotBlank()) nombre = nombreState }
    LaunchedEffect(emailState) { if (emailState.isNotBlank()) email = emailState }
    LaunchedEffect(passwordState) {
        if (passwordState.isNotBlank()) {
            password = passwordState
            confirmPassword = passwordState
        }
    }

    val focusManager = LocalFocusManager.current
    val haptic = rememberHaptic()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Posición (en px) de cada campo dentro del formulario, para llevar al usuario al que tiene error
    val posiciones = remember { mutableStateMapOf<String, Int>() }
    fun Modifier.marcar(clave: String) = this.onGloballyPositioned { posiciones[clave] = it.positionInParent().y.toInt() }
    fun irA(clave: String) {
        posiciones[clave]?.let { y -> scope.launch { scrollState.animateScrollTo((y - 40).coerceAtLeast(0)) } }
    }

    // ---------- Validaciones ----------
    val isNombreValid = nombre.trim().length >= 2 && nombre.none { it.isDigit() }
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val telefonoLimpio = telefono.filter { it.isDigit() || it == '+' }
    val isTelefonoValid = telefono.isBlank() || telefonoLimpio.length in 7..15
    val tieneLargo = password.length >= 6
    val tieneNumero = password.any { it.isDigit() }
    val tieneLetra = password.any { it.isLetter() }
    val isPasswordValid = tieneLargo
    val passwordsCoinciden = confirmPassword.isNotEmpty() && confirmPassword == password

    // Progreso del formulario (para que la persona sepa cuánto le falta)
    val pasosObligatorios = listOf(isNombreValid, isEmailValid, isPasswordValid, passwordsCoinciden, aceptaTerminos)
    val completados = pasosObligatorios.count { it }

    fun leerResumen() {
        val faltantes = buildList {
            if (!isNombreValid) add("tu nombre")
            if (!isEmailValid) add("un correo válido")
            if (!isPasswordValid) add("una contraseña de al menos seis caracteres")
            if (!passwordsCoinciden) add("confirmar la contraseña")
            if (!aceptaTerminos) add("aceptar los términos")
        }
        if (faltantes.isEmpty()) onSpeak("Todo está listo. Toca Crear cuenta.")
        else onSpeak("Llevas $completados de 5 pasos. Te falta: ${faltantes.joinToString(", ")}.")
    }

    fun handleRegister() {
        intentoEnviar = true
        focusManager.clearFocus()
        val (error, campo) = when {
            !isNombreValid -> "Escribe tu nombre (mínimo 2 letras, sin números)." to "nombre"
            !isEmailValid -> "El correo no tiene un formato válido. Ejemplo: nombre@correo.com" to "email"
            !isTelefonoValid -> "El teléfono debe tener entre 7 y 15 dígitos." to "telefono"
            !isPasswordValid -> "La contraseña debe tener al menos 6 caracteres." to "password"
            !passwordsCoinciden -> "Las contraseñas no coinciden. Escríbela igual en los dos campos." to "confirm"
            !aceptaTerminos -> "Debes aceptar los términos y condiciones para continuar." to "terminos"
            else -> null to null
        }
        if (error != null) {
            errorMessage = error
            onSpeak(error)
            campo?.let { irA(it) } // baja (o sube) solo hasta el campo con el problema
            return
        }
        errorMessage = null
        isLoading = true
        onSpeak("Creando tu cuenta.")
        onRegisterSubmit(
            RegistroFormulario(
                nombreCompleto = nombre.trim(),
                email = email.trim(),
                password = password,
                telefono = telefonoLimpio.ifBlank { null },
                nivelVision = nivelVision
            )
        ) { err ->
            if (err != null) {
                isLoading = false
                errorMessage = err
                onSpeak(err)
            } else {
                errorMessage = null
                successMessage = "¡Registro exitoso! Creando tu cuenta..."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkTealBg)
    ) {
        AppHeader(
            titleBanner = "CREAR CUENTA",
            showBack = true,
            onBackClick = onBackClick,
            onSpeakerClick = { leerResumen() },
            onNotificationClick = { onSpeak("Sin notificaciones pendientes.") }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(TealHeader)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Únete a Unlimited Vision",
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "Solo toma un minuto",
                fontSize = 14.sp,
                color = Color.LightGray
            )

            // Barra de progreso del formulario
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { haptic(); leerResumen() }
                    .padding(4.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progreso", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("$completados de 5", color = LinkCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { completados / 5f },
                    color = LinkCyan,
                    trackColor = Color(0xFF2C3E47),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(Modifier.height(20.dp))

            // ---------- Datos personales ----------
            SectionTitle("Tus datos")

            AccessibleTextField(
                modifier = Modifier.marcar("nombre"),
                label = "Nombre completo",
                value = nombre,
                onValueChange = { nombre = it; errorMessage = null },
                leadingIcon = Icons.Default.Person,
                placeholder = "Ej: Pablo Leyton",
                onSpeak = onSpeak,
                speakHint = "Escribe tu nombre y apellido.",
                isValid = if (nombre.isBlank() && !intentoEnviar) null else isNombreValid,
                errorText = "Mínimo 2 letras y sin números.",
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(14.dp))

            AccessibleTextField(
                modifier = Modifier.marcar("email"),
                label = "Correo electrónico",
                value = email,
                onValueChange = { email = it; errorMessage = null },
                leadingIcon = Icons.Default.Email,
                placeholder = "ejemplo@correo.com",
                onSpeak = onSpeak,
                speakHint = "Será tu usuario para entrar a la app.",
                isValid = if (email.isBlank() && !intentoEnviar) null else isEmailValid,
                errorText = "Revisa el correo, falta la @ o el dominio.",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(14.dp))

            AccessibleTextField(
                modifier = Modifier.marcar("telefono"),
                label = "Teléfono",
                value = telefono,
                onValueChange = { telefono = it; errorMessage = null },
                leadingIcon = Icons.Default.Phone,
                placeholder = "300 123 4567",
                onSpeak = onSpeak,
                speakHint = "Es opcional. Sirve para contactarte en una emergencia.",
                isValid = if (telefono.isBlank()) null else isTelefonoValid,
                errorText = "Entre 7 y 15 dígitos.",
                helperText = "Para alertas SOS y contacto de emergencia",
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                optional = true
            )

            // ---------- Nivel de visión ----------
            Spacer(Modifier.height(22.dp))
            SectionTitle("¿Cómo usarás la app?", subtitle = "Opcional · Nos ayuda a adaptar la experiencia")
            nivelesVision.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    fila.forEach { op ->
                        SelectableOptionCard(
                            text = op.texto,
                            icon = op.icono,
                            selected = nivelVision == op.valor,
                            onClick = {
                                nivelVision = if (nivelVision == op.valor) null else op.valor
                                onSpeak(if (nivelVision == op.valor) "${op.texto}, seleccionado." else "${op.texto}, deseleccionado.")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---------- Seguridad ----------
            Spacer(Modifier.height(10.dp))
            SectionTitle("Seguridad")

            AccessibleTextField(
                modifier = Modifier.marcar("password"),
                label = "Contraseña",
                value = password,
                onValueChange = { password = it; errorMessage = null },
                leadingIcon = Icons.Default.Lock,
                placeholder = "Mínimo 6 caracteres",
                onSpeak = onSpeak,
                speakHint = "Crea una contraseña de al menos seis caracteres. Mejor si mezcla letras y números.",
                isValid = if (password.isEmpty() && !intentoEnviar) null else isPasswordValid,
                errorText = "Mínimo 6 caracteres.",
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                isPassword = true
            )
            PasswordStrengthMeter(password)
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp)) {
                RequirementRow("Al menos 6 caracteres", tieneLargo)
                RequirementRow("Contiene letras", tieneLetra)
                RequirementRow("Contiene números (recomendado)", tieneNumero)
            }

            Spacer(Modifier.height(14.dp))

            AccessibleTextField(
                modifier = Modifier.marcar("confirm"),
                label = "Confirmar contraseña",
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                leadingIcon = Icons.Default.LockReset,
                placeholder = "Repite tu contraseña",
                onSpeak = { texto ->
                    onSpeak(
                        texto + if (confirmPassword.isEmpty()) "" else if (passwordsCoinciden) " Las contraseñas coinciden." else " Las contraseñas no coinciden."
                    )
                },
                speakHint = "Escribe otra vez la misma contraseña.",
                isValid = if (confirmPassword.isEmpty() && !intentoEnviar) null else passwordsCoinciden,
                errorText = "Las contraseñas no coinciden.",
                helperText = if (passwordsCoinciden) "✓ Las contraseñas coinciden" else null,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() },
                isPassword = true
            )

            // ---------- Términos ----------
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .marcar("terminos")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic()
                        aceptaTerminos = !aceptaTerminos
                        onSpeak(if (aceptaTerminos) "Términos aceptados." else "Términos no aceptados.")
                    }
                    .padding(vertical = 6.dp)
            ) {
                Checkbox(
                    checked = aceptaTerminos,
                    onCheckedChange = { haptic(); aceptaTerminos = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = LinkCyan,
                        uncheckedColor = Color.LightGray,
                        checkmarkColor = Color.Black
                    )
                )
                Text(
                    text = buildAnnotatedString {
                        append("Acepto los ")
                        withStyle(SpanStyle(color = LinkCyan, fontWeight = FontWeight.Bold)) { append("términos y condiciones") }
                        append(" y el tratamiento de mis datos.")
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            TextButton(onClick = { haptic(); showTerms = true }, modifier = Modifier.align(Alignment.Start)) {
                Text("Leer términos", color = LinkCyan, fontSize = 13.sp)
            }

            OrDivider()

            SecondaryActionButton(
                text = "Registrarme con voz",
                icon = Icons.Default.Mic,
                onClick = {
                    errorMessage = null
                    onVoiceRegister()
                }
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = buildAnnotatedString {
                    append("¿Ya tienes una cuenta? ")
                    withStyle(SpanStyle(color = LinkCyan, fontWeight = FontWeight.Bold)) { append("Inicia sesión") }
                },
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { haptic(); onNavigateToLogin() }
                    .padding(12.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // ---------- Barra inferior fija: el botón siempre está visible ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B282E))
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            ErrorBanner(errorMessage, onSpeak)
            SuccessBanner(successMessage, onSpeak)
            if (errorMessage != null || successMessage != null) Spacer(Modifier.height(10.dp))
            PrimaryActionButton(
                text = if (completados == 5) "Crear cuenta" else "Crear cuenta ($completados/5)",
                onClick = { handleRegister() },
                isLoading = isLoading,
                containerColor = LightBlueButton,
                icon = Icons.Default.PersonAdd
            )
        }
    }

    if (showTerms) {
        val textoTerminos = "Unlimited Vision usa tu nombre, correo y teléfono solo para darte el servicio. " +
            "Tus rutas se guardan en tu cuenta para que puedas repetirlas fácilmente. " +
            "Tu contraseña se guarda cifrada y nunca se comparte. Puedes eliminar tu cuenta cuando quieras."
        LaunchedEffect(Unit) { onSpeak("Términos y condiciones. $textoTerminos") }
        AlertDialog(
            onDismissRequest = { showTerms = false },
            containerColor = com.example.unlimitedvision.theme.CardBgTeal,
            title = { Text("Términos y condiciones", color = Color.White, fontFamily = FontFamily.Serif) },
            text = { Text(textoTerminos, color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    aceptaTerminos = true
                    showTerms = false
                    onSpeak("Términos aceptados.")
                }) { Text("Acepto", color = LinkCyan, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTerms = false }) { Text("Cerrar", color = Color.LightGray) }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle != null) Text(subtitle, color = Color.LightGray, fontSize = 12.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    UnlimitedVisionTheme {
        RegisterScreen(
            onBackClick = {},
            onRegisterSubmit = { _, _ -> },
            onSpeak = {}
        )
    }
}
