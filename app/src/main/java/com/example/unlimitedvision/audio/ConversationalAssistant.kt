package com.example.unlimitedvision.audio

import android.content.Context
import android.os.Handler
import android.os.Looper

enum class AssistantStep {
    IDLE,
    GREETING,
    REGISTER_NAME,
    REGISTER_NAME_CONFIRM,
    REGISTER_LASTNAME,
    REGISTER_LASTNAME_CONFIRM,
    REGISTER_EMAIL,
    REGISTER_EMAIL_CONFIRM,
    REGISTER_PASSWORD,
    LOGIN_EMAIL,
    LOGIN_PASSWORD,
    MAIN_CONVERSATIONAL
}

class ConversationalAssistant(
    private val context: Context,
    private val audioAssistant: AudioAssistant,
    private val onNavigate: (String) -> Unit,
    private val onUpdateNameField: (String) -> Unit,
    private val onUpdateEmailField: (String) -> Unit,
    private val onUpdatePasswordField: (String) -> Unit,
    // Se llaman cuando el usuario terminó de dictar los datos; MainActivity los envía al backend
    private val onRegisterCompleted: (name: String, email: String, password: String) -> Unit,
    private val onLoginCompleted: (email: String, password: String) -> Unit
) {

    private val voiceListener: VoiceCommandListener = VoiceCommandListener(
        context = context,
        onCommandDetected = { input -> processUserInput(input) },
        onErrorDetected = { code -> handleSpeechError(code) },
        onPartialText = { partial -> updateFieldsPartial(partial) }
    )

    var currentStep: AssistantStep = AssistantStep.IDLE
        private set

    // Temporary registration data
    private var tempFirstName = ""
    private var tempLastName = ""
    private var tempEmail = ""
    private var tempPassword = ""

    private val mainHandler = Handler(Looper.getMainLooper())

    fun startWelcomeFlow() {
        currentStep = AssistantStep.GREETING
        speakAndListen(
            "Hola, ¿cómo estás? Bienvenido a Unlimited Vision, tu asistente por voz. " +
                "En esta pantalla tienes el botón para ingresar y el botón para registrarte. " +
                "Dime cuál quieres: iniciar sesión o registrarme."
        )
    }

    fun startRegisterFlow() {
        currentStep = AssistantStep.REGISTER_NAME
        tempFirstName = ""
        tempLastName = ""
        tempEmail = ""
        tempPassword = ""
        onUpdateNameField("")
        onUpdateEmailField("")
        onUpdatePasswordField("")
        speakAndListen("Excelente, vamos a crear tu cuenta. Por favor dime tu primer nombre.")
    }

    fun startLoginFlow() {
        currentStep = AssistantStep.LOGIN_EMAIL
        tempEmail = ""
        tempPassword = ""
        onUpdateEmailField("")
        onUpdatePasswordField("")
        speakAndListen("Inicio de sesión guiado. Por favor dime tu correo electrónico diciéndolo o deletreándolo.")
    }

    private var erroresSeguidos = 0

    private fun speakAndListen(text: String) {
        audioAssistant.speak(text) {
            mainHandler.postDelayed({
                voiceListener.startListening()
            }, 400)
        }
    }

    private fun handleSpeechError(code: Int) {
        if (currentStep == AssistantStep.IDLE) return
        erroresSeguidos++
        when {
            code == android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                audioAssistant.speak("No tengo permiso para usar el micrófono. Actívalo en los ajustes de la aplicación.")
            erroresSeguidos > 2 -> {
                // Para no quedar en un ciclo infinito: espera a que toquen el botón del micrófono
                erroresSeguidos = 0
                audioAssistant.speak("No logro escucharte. Toca el botón del micrófono cuando quieras hablar.")
            }
            else -> speakAndListen("No te escuché bien. ¿Me lo repites?")
        }
    }

    private fun updateFieldsPartial(text: String) {
        when (currentStep) {
            AssistantStep.REGISTER_NAME -> onUpdateNameField(text)
            AssistantStep.REGISTER_EMAIL, AssistantStep.LOGIN_EMAIL -> onUpdateEmailField(text)
            AssistantStep.REGISTER_PASSWORD, AssistantStep.LOGIN_PASSWORD -> onUpdatePasswordField(text)
            else -> {}
        }
    }

    fun processUserInput(input: String) {
        erroresSeguidos = 0
        val text = input.trim().lowercase()

        // En cualquier paso del registro o login, "cancelar" o "cerrar" vuelve al inicio
        if (currentStep != AssistantStep.MAIN_CONVERSATIONAL && currentStep != AssistantStep.IDLE &&
            (text == "cancelar" || text == "cerrar" || text == "volver")
        ) {
            onNavigate("welcome")
            startWelcomeFlow()
            return
        }

        when (currentStep) {
            AssistantStep.GREETING -> {
                when {
                    text.contains("registr") || text.contains("crear") || text.contains("cuenta") || text.contains("nuevo") -> {
                        onNavigate("register")
                        startRegisterFlow()
                    }
                    text.contains("inici") || text.contains("ingres") || text.contains("entrar") || text.contains("sesion") || text.contains("sesión") -> {
                        onNavigate("login")
                        startLoginFlow()
                    }
                    else -> {
                        speakAndListen("No te entendí bien. ¿Deseas iniciar sesión o registrarte?")
                    }
                }
            }

            AssistantStep.REGISTER_NAME -> {
                tempFirstName = input.replace("mi nombre es", "").replace("me llamo", "").trim().replaceFirstChar { it.uppercase() }
                onUpdateNameField(tempFirstName)
                currentStep = AssistantStep.REGISTER_NAME_CONFIRM
                speakAndListen("Entendí $tempFirstName. ¿Es correcto tu primer nombre? Di sí para continuar o no para repetir.")
            }

            AssistantStep.REGISTER_NAME_CONFIRM -> {
                if (text.contains("sí") || text.contains("si") || text.contains("correcto") || text.contains("está bien")) {
                    currentStep = AssistantStep.REGISTER_LASTNAME
                    speakAndListen("Perfecto $tempFirstName. Ahora dime tu primer apellido.")
                } else {
                    currentStep = AssistantStep.REGISTER_NAME
                    speakAndListen("De acuerdo. Por favor dime nuevamente tu primer nombre o deletréalo letra por letra.")
                }
            }

            AssistantStep.REGISTER_LASTNAME -> {
                tempLastName = input.replace("mi apellido es", "").trim().replaceFirstChar { it.uppercase() }
                val fullName = "$tempFirstName $tempLastName".trim()
                onUpdateNameField(fullName)
                currentStep = AssistantStep.REGISTER_LASTNAME_CONFIRM
                speakAndListen("Entendí el apellido $tempLastName. ¿Es correcto? Di sí o no.")
            }

            AssistantStep.REGISTER_LASTNAME_CONFIRM -> {
                if (text.contains("sí") || text.contains("si") || text.contains("correcto")) {
                    currentStep = AssistantStep.REGISTER_EMAIL
                    speakAndListen("Muy bien $tempFirstName $tempLastName. Ahora dime tu correo electrónico.")
                } else {
                    currentStep = AssistantStep.REGISTER_LASTNAME
                    speakAndListen("De acuerdo, dime de nuevo tu primer apellido.")
                }
            }

            AssistantStep.REGISTER_EMAIL -> {
                tempEmail = parseEmailFromSpeech(input)
                onUpdateEmailField(tempEmail)
                currentStep = AssistantStep.REGISTER_EMAIL_CONFIRM
                speakAndListen("Entendí el correo $tempEmail. ¿Es correcto? Di sí o no.")
            }

            AssistantStep.REGISTER_EMAIL_CONFIRM -> {
                if (text.contains("sí") || text.contains("si") || text.contains("correcto")) {
                    currentStep = AssistantStep.REGISTER_PASSWORD
                    speakAndListen("Excelente. Por último, dime tu contraseña para la cuenta.")
                } else {
                    currentStep = AssistantStep.REGISTER_EMAIL
                    speakAndListen("De acuerdo, dime nuevamente tu correo electrónico.")
                }
            }

            AssistantStep.REGISTER_PASSWORD -> {
                tempPassword = parsePasswordFromSpeech(input)
                if (tempPassword.length < 6) {
                    speakAndListen("La contraseña debe tener mínimo 6 caracteres. Dímela de nuevo, por ejemplo seis números.")
                    return
                }
                onUpdatePasswordField(tempPassword)
                val fullName = "$tempFirstName $tempLastName".trim()
                currentStep = AssistantStep.IDLE
                audioAssistant.speak("Un momento, estoy creando tu cuenta.")
                onRegisterCompleted(fullName, tempEmail, tempPassword)
            }

            AssistantStep.LOGIN_EMAIL -> {
                tempEmail = parseEmailFromSpeech(input)
                onUpdateEmailField(tempEmail)
                currentStep = AssistantStep.LOGIN_PASSWORD
                speakAndListen("Entendí $tempEmail. Ahora dime tu contraseña.")
            }

            AssistantStep.LOGIN_PASSWORD -> {
                tempPassword = parsePasswordFromSpeech(input)
                onUpdatePasswordField(tempPassword)
                currentStep = AssistantStep.IDLE
                audioAssistant.speak("Un momento, estoy verificando tus datos.")
                onLoginCompleted(tempEmail, tempPassword)
            }

            AssistantStep.MAIN_CONVERSATIONAL, AssistantStep.IDLE -> {
                processGlobalCommands(text)
            }
        }
    }

    private fun processGlobalCommands(text: String) {
        when {
            text.contains("transporte") || text.contains("bus") || text.contains("ruta") || text.contains("parada") -> {
                onNavigate("transport")
                speakAndListen("Abriendo transporte y rutas del SITP. Tienes paradas cercanas a 192 metros. Toca una parada para iniciar guía peatonal.")
            }
            text.contains("cámara") || text.contains("camara") || text.contains("foto") || text.contains("ver") || text.contains("enfrente") -> {
                onNavigate("camera")
                speakAndListen("Cámara de visión aumentada activa. Escaneando objetos y restaurantes a tu alrededor.")
            }
            text.contains("menú") || text.contains("menu") || text.contains("ubicación") || text.contains("ubicacion") || text.contains("inicio") -> {
                onNavigate("main_menu")
                speakAndListen("Menú principal de ubicación. Tienes botones rápidos para tomar transporte o abrir cámara.")
            }
            text.contains("perfil") || text.contains("mis datos") || text.contains("ajustes") || text.contains("configuración") -> {
                onNavigate("profile")
                speakAndListen("Abriendo tu perfil. Aquí puedes cambiar tus datos, tu contacto de emergencia y la velocidad de mi voz.")
            }
            text.contains("sos") || text.contains("emergencia") || text.contains("ayuda") -> {
                onNavigate("main_menu")
                speakAndListen("Alerta de emergencia SOS activada. Compartiendo tu ubicación GPS en vivo.")
            }
            text.contains("cerrar") || text.contains("salir") -> {
                onNavigate("welcome")
                currentStep = AssistantStep.GREETING
                speakAndListen("Sesión cerrada. ¿Deseas iniciar sesión o registrarte?")
            }
            else -> {
                speakAndListen("Te escucho. Puedes decirme: transporte, cámara, ubicación, perfil o SOS.")
            }
        }
    }

    // ---------- Respuestas del backend (las llama MainActivity) ----------

    /** Registro o login correcto: saluda y queda escuchando comandos */
    fun onAuthSuccess(nombre: String, esRegistro: Boolean) {
        currentStep = AssistantStep.MAIN_CONVERSATIONAL
        val saludo = if (esRegistro) "¡Registro exitoso! Cuenta creada para $nombre." else "¡Inicio de sesión exitoso! Bienvenido de nuevo, $nombre."
        speakAndListen("$saludo Puedes decirme qué deseas hacer: transporte, cámara o ubicación.")
    }

    /** El backend rechazó el registro */
    fun onRegisterFailed(mensaje: String, correoYaExiste: Boolean) {
        if (correoYaExiste) {
            currentStep = AssistantStep.REGISTER_EMAIL
            speakAndListen("Ya existe una cuenta con el correo $tempEmail. Dime otro correo electrónico, o di cerrar para volver al inicio.")
        } else {
            currentStep = AssistantStep.REGISTER_NAME
            speakAndListen("No pude crear la cuenta. $mensaje. Empecemos de nuevo: dime tu primer nombre.")
        }
    }

    /** El backend rechazó el login (correo o contraseña mal, o servidor apagado) */
    fun onLoginFailed(mensaje: String) {
        currentStep = AssistantStep.LOGIN_EMAIL
        tempEmail = ""
        tempPassword = ""
        onUpdateEmailField("")
        onUpdatePasswordField("")
        speakAndListen("$mensaje. Intentemos otra vez: dime tu correo electrónico.")
    }

    /** Quita espacios: "1 2 3 4 5 6" -> "123456" */
    private fun parsePasswordFromSpeech(input: String): String =
        input.trim().replace(" ", "")

    private fun parseEmailFromSpeech(input: String): String {
        var email = input.lowercase()
            .replace(" arroba ", "@")
            .replace(" arroba", "@")
            .replace("punto com", ".com")
            .replace("punto ", ".")
            .replace(" ", "")
        if (!email.contains("@")) {
            email = "$email@gmail.com"
        }
        return email
    }

    /** Modo acompañante: deja de escuchar y queda en reposo */
    fun silence() {
        currentStep = AssistantStep.IDLE
        voiceListener.stopListening()
    }

    fun startListeningManual() {
        voiceListener.startListening()
    }

    fun stop() {
        voiceListener.destroy()
    }
}
