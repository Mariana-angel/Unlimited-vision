package com.example.unlimitedvision.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unlimitedvision.theme.CardBgTeal
import com.example.unlimitedvision.theme.DarkOliveButton
import com.example.unlimitedvision.theme.LinkCyan
import com.example.unlimitedvision.theme.OliveButtonText
import com.example.unlimitedvision.theme.TealHeader

// ---------- Colores de apoyo (alto contraste) ----------
val FieldBg = Color(0xFFD4E3E5)
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFFF6B6B)
val WarningAmber = Color(0xFFFFB74D)

/** Vibración corta: confirma al usuario que tocó algo (clave para personas con baja visión) */
@Composable
fun rememberHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
}

/**
 * Campo de texto accesible: etiqueta grande, icono, botón para escuchar el campo,
 * mensaje de error/ayuda debajo y estado válido/inválido.
 */
@Composable
fun AccessibleTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    placeholder: String,
    onSpeak: (String) -> Unit,
    speakHint: String,
    modifier: Modifier = Modifier,
    isValid: Boolean? = null,          // null = sin validar todavía
    errorText: String? = null,
    helperText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    optional: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val borderColor by animateColorAsState(
        when (isValid) {
            true -> SuccessGreen
            false -> ErrorRed
            null -> Color.Transparent
        },
        label = "border"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            if (optional) {
                Text("  (opcional)", fontSize = 12.sp, color = Color.LightGray)
            }
            Spacer(Modifier.weight(1f))
            // Escuchar qué va en este campo y qué tiene escrito
            IconButton(
                onClick = {
                    haptic()
                    val contenido = when {
                        value.isBlank() -> "Está vacío."
                        isPassword -> "Tiene ${value.length} caracteres."
                        else -> "Dice: $value."
                    }
                    onSpeak("$label. $speakHint $contenido")
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Escuchar campo $label",
                    tint = LinkCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            singleLine = true,
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = Color(0xFF2C3E47)) },
            trailingIcon = {
                when {
                    isPassword -> IconButton(onClick = { haptic(); passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = Color.DarkGray
                        )
                    }
                    isValid == true -> Icon(Icons.Default.CheckCircle, "Campo válido", tint = Color(0xFF2E7D32))
                    isValid == false -> Icon(Icons.Default.ErrorOutline, "Campo con error", tint = Color(0xFFC62828))
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = if (isValid == null) Color.White else borderColor,
                unfocusedBorderColor = borderColor,
                cursorColor = Color.Black
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 17.sp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
        )

        val bottom = if (isValid == false && errorText != null) errorText else helperText
        AnimatedVisibility(visible = bottom != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Text(
                text = bottom ?: "",
                fontSize = 12.sp,
                color = if (isValid == false && errorText != null) ErrorRed else Color.LightGray,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

/** Fuerza de la contraseña: 0 = vacía, 1 débil, 2 media, 3 buena, 4 fuerte */
fun passwordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var puntos = 0
    if (password.length >= 6) puntos++
    if (password.length >= 10) puntos++
    if (password.any { it.isDigit() } && password.any { it.isLetter() }) puntos++
    if (password.any { it.isUpperCase() } || password.any { !it.isLetterOrDigit() }) puntos++
    return puntos.coerceIn(1, 4)
}

fun passwordStrengthLabel(level: Int) = when (level) {
    1 -> "Débil"
    2 -> "Media"
    3 -> "Buena"
    4 -> "Fuerte"
    else -> ""
}

@Composable
fun PasswordStrengthMeter(password: String) {
    val level = passwordStrength(password)
    val color = when (level) {
        1 -> ErrorRed
        2 -> WarningAmber
        3 -> Color(0xFF9CCC65)
        else -> SuccessGreen
    }
    val progress by animateFloatAsState(level / 4f, tween(400), label = "strength")

    AnimatedVisibility(visible = password.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = color,
                    trackColor = Color(0xFF2C3E47),
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Seguridad: ${passwordStrengthLabel(level)}",
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Una línea de requisito con check verde o círculo vacío */
@Composable
fun RequirementRow(text: String, met: Boolean) {
    val color by animateColorAsState(if (met) SuccessGreen else Color.LightGray, label = "req")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .semantics { contentDescription = "$text: ${if (met) "cumplido" else "pendiente"}" }
    ) {
        Icon(
            if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = color, fontSize = 12.sp)
    }
}

/** Tarjeta de error visible y con alto contraste */
@Composable
fun ErrorBanner(message: String?, onSpeak: (String) -> Unit) {
    AnimatedVisibility(visible = message != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF4A2326))
                .border(1.dp, ErrorRed, RoundedCornerShape(12.dp))
                .clickable { message?.let(onSpeak) }
                .padding(12.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed)
            Spacer(Modifier.width(10.dp))
            Text(message ?: "", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
    }
}

/** Tarjeta de éxito bonita con verde de alto contraste e icono de check */
@Composable
fun SuccessBanner(message: String?, onSpeak: ((String) -> Unit)? = null) {
    AnimatedVisibility(visible = message != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1B3E2B))
                .border(1.5.dp, SuccessGreen, RoundedCornerShape(12.dp))
                .clickable { message?.let { onSpeak?.invoke(it) } }
                .padding(14.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text(message ?: "", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}

/** Botón principal grande con indicador de carga */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = Color(0xFFE5EFEF),
    contentColor: Color = Color.Black,
    icon: ImageVector? = null
) {
    val haptic = rememberHaptic()
    Button(
        onClick = { haptic(); onClick() },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = contentColor, strokeWidth = 2.5.dp)
            Spacer(Modifier.width(12.dp))
            Text("Un momento...", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
    }
}

/** Botón secundario (contorno) */
@Composable
fun SecondaryActionButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = rememberHaptic()
    OutlinedButton(
        onClick = { haptic(); onClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, LinkCyan),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(icon, contentDescription = null, tint = LinkCyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Botón de micrófono que "respira" para que se note que es el control principal de voz */
@Composable
fun PulsingMicButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier, size: Int = 84) {
    val haptic = rememberHaptic()
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "alpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size((size * 1.45f).dp)) {
            Box(
                Modifier
                    .size(size.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(LinkCyan.copy(alpha = alpha))
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(TealHeader)
                    .border(3.dp, LinkCyan, CircleShape)
                    .clickable { haptic(); onClick() }
                    .semantics { contentDescription = label }
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size((size * 0.45f).dp))
            }
        }
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** Separador "o" entre opciones */
@Composable
fun OrDivider(text: String = "o") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFF4A626C))
        Text("  $text  ", color = Color.LightGray, fontSize = 13.sp)
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFF4A626C))
    }
}

/** Chip grande para elegir una opción (ej. nivel de visión) */
@Composable
fun SelectableOptionCard(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHaptic()
    val bg by animateColorAsState(if (selected) TealHeader else CardBgTeal, label = "chipBg")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(2.dp, if (selected) LinkCyan else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable { haptic(); onClick() }
            .semantics { contentDescription = "$text${if (selected) ", seleccionado" else ""}" }
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.White else Color.LightGray, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** Botón píldora estilo original de la app (oliva) */
@Composable
fun OlivePillButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = rememberHaptic()
    Button(
        onClick = { haptic(); onClick() },
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkOliveButton),
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Icon(icon, contentDescription = null, tint = OliveButtonText, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = OliveButtonText, fontFamily = FontFamily.Serif)
    }
}
