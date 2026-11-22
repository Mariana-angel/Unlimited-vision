package com.example.unlimitedvision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unlimitedvision.audio.AudioAssistant
import com.example.unlimitedvision.audio.ConversationalAssistant
import com.example.unlimitedvision.audio.VoiceSettings
import com.example.unlimitedvision.data.UserPreferences
import com.example.unlimitedvision.network.ApiClient
import com.example.unlimitedvision.network.ApiResult
import com.example.unlimitedvision.network.AuthRepository
import com.example.unlimitedvision.network.AuthResponse
import com.example.unlimitedvision.screens.*
import com.example.unlimitedvision.theme.UnlimitedVisionTheme

class MainActivity : ComponentActivity() {

    private lateinit var audioAssistant: AudioAssistant
    private lateinit var userPreferences: UserPreferences
    private lateinit var conversationalAssistant: ConversationalAssistant
    private val authRepository = AuthRepository()

    // El saludo por voz se lanza una sola vez, cuando hay voz lista y permiso de micrófono
    private var startsOnWelcome = false
    private var welcomeGreetingDone = false

    private fun tryStartWelcomeGreeting() {
        if (!startsOnWelcome || welcomeGreetingDone || !::conversationalAssistant.isInitialized) return
        val micOk = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!micOk) return
        welcomeGreetingDone = true
        audioAssistant.whenReady { conversationalAssistant.startWelcomeFlow() }
    }

    /** Guarda la sesión que devolvió el backend (token + datos del usuario) */
    private fun guardarSesion(auth: AuthResponse, remember: Boolean) {
        ApiClient.token = auth.accessToken
        userPreferences.saveToken(auth.accessToken)
        userPreferences.saveUser(auth.usuario.nombre, auth.usuario.email, remember)
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Si ya dieron permiso de micrófono, arranca el saludo del asistente
        tryStartWelcomeGreeting()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioAssistant = AudioAssistant(this)
        userPreferences = UserPreferences(this)

        // Modo acompañante: recuerda si la voz estaba apagada
        VoiceSettings.init(this)
        VoiceSettings.onChange = { activo ->
            if (activo) {
                audioAssistant.speak("Asistente de voz activado.")
            } else {
                audioAssistant.stopSpeaking()
                if (::conversationalAssistant.isInitialized) conversationalAssistant.silence()
                android.widget.Toast.makeText(this, "Modo acompañante: voz desactivada", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // Request initial Location, Camera, and Record Audio permissions
        checkAndRequestPermissions()

        // Si ya había sesión guardada, recupera el token para las peticiones
        ApiClient.token = userPreferences.getToken()
        val haySesion = userPreferences.isLoggedIn() && ApiClient.token != null
        val startDest = if (haySesion) "main_menu" else "welcome"
        startsOnWelcome = startDest == "welcome"

        setContent {
            UnlimitedVisionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var currentUserName by remember { mutableStateOf(userPreferences.getUserName()) }

                    var voiceNombreState by remember { mutableStateOf("") }
                    var voiceEmailState by remember { mutableStateOf("") }
                    var voicePasswordState by remember { mutableStateOf("") }

                    // Initialize Conversational Voice Assistant
                    remember {
                        conversationalAssistant = ConversationalAssistant(
                            context = this@MainActivity,
                            audioAssistant = audioAssistant,
                            onNavigate = { destination ->
                                navController.navigate(destination) {
                                    if (destination == "welcome") popUpTo(0) { inclusive = true }
                                }
                            },
                            onUpdateNameField = { name -> voiceNombreState = name },
                            onUpdateEmailField = { email -> voiceEmailState = email },
                            onUpdatePasswordField = { pass -> voicePasswordState = pass },
                            onRegisterCompleted = { name, email, password ->
                                lifecycleScope.launch {
                                    when (val r = authRepository.registro(name, email, password)) {
                                        is ApiResult.Exito -> {
                                            guardarSesion(r.data, remember = true)
                                            currentUserName = r.data.usuario.nombre
                                            navController.navigate("main_menu") {
                                                popUpTo("welcome") { inclusive = true }
                                            }
                                            conversationalAssistant.onAuthSuccess(r.data.usuario.nombre, esRegistro = true)
                                        }
                                        is ApiResult.Error -> conversationalAssistant.onRegisterFailed(
                                            r.mensaje, correoYaExiste = r.codigo == 409
                                        )
                                    }
                                }
                            },
                            onLoginCompleted = { email, password ->
                                lifecycleScope.launch {
                                    when (val r = authRepository.login(email, password)) {
                                        is ApiResult.Exito -> {
                                            guardarSesion(r.data, remember = true)
                                            currentUserName = r.data.usuario.nombre
                                            navController.navigate("main_menu") {
                                                popUpTo("welcome") { inclusive = true }
                                            }
                                            conversationalAssistant.onAuthSuccess(r.data.usuario.nombre, esRegistro = false)
                                        }
                                        is ApiResult.Error -> conversationalAssistant.onLoginFailed(r.mensaje)
                                    }
                                }
                            }
                        )
                        true
                    }

                    // Saludo inicial del asistente de voz en la pantalla de bienvenida
                    LaunchedEffect(Unit) { tryStartWelcomeGreeting() }

                    NavHost(
                        navController = navController,
                        startDestination = startDest
                    ) {
                        composable("welcome") {
                            WelcomeScreen(
                                onNavigateToLogin = {
                                    navController.navigate("login")
                                    conversationalAssistant.startLoginFlow()
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                    conversationalAssistant.startRegisterFlow()
                                },
                                onSpeak = { text -> audioAssistant.speak(text) },
                                onMicClick = { conversationalAssistant.startListeningManual() }
                            )
                        }

                        composable("login") {
                            LoginScreen(
                                emailState = voiceEmailState,
                                passwordState = voicePasswordState,
                                onBackClick = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate("welcome") { popUpTo(0) }
                                    }
                                },
                                onLoginSubmit = { email, password, remember, onResult ->
                                    lifecycleScope.launch {
                                        when (val r = authRepository.login(email, password)) {
                                            is ApiResult.Exito -> {
                                                onResult(null)
                                                guardarSesion(r.data, remember)
                                                currentUserName = r.data.usuario.nombre
                                                audioAssistant.speak("¡Inicio de sesión exitoso! Bienvenido de nuevo, ${r.data.usuario.nombre}.")
                                                kotlinx.coroutines.delay(1200)
                                                navController.navigate("main_menu") {
                                                    popUpTo("welcome") { inclusive = true }
                                                }
                                            }
                                            is ApiResult.Error -> onResult(r.mensaje)
                                        }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onSpeak = { text -> audioAssistant.speak(text) },
                                onVoiceLogin = { conversationalAssistant.startLoginFlow() }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                nombreState = voiceNombreState,
                                emailState = voiceEmailState,
                                passwordState = voicePasswordState,
                                onBackClick = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate("welcome") { popUpTo(0) }
                                    }
                                },
                                onRegisterSubmit = { form, onResult ->
                                    lifecycleScope.launch {
                                        when (val r = authRepository.registro(form)) {
                                            is ApiResult.Exito -> {
                                                onResult(null)
                                                guardarSesion(r.data, remember = true)
                                                currentUserName = r.data.usuario.nombre
                                                audioAssistant.speak("¡Registro exitoso! Cuenta creada. Bienvenido, ${r.data.usuario.nombre}.")
                                                kotlinx.coroutines.delay(1200)
                                                navController.navigate("main_menu") {
                                                    popUpTo("welcome") { inclusive = true }
                                                }
                                            }
                                            is ApiResult.Error -> onResult(r.mensaje)
                                        }
                                    }
                                },
                                onSpeak = { text -> audioAssistant.speak(text) },
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                },
                                onVoiceRegister = { conversationalAssistant.startRegisterFlow() }
                            )
                        }

                        composable("main_menu") {
                            MainMenuScreen(
                                onBackClick = { navController.popBackStack() },
                                onNavigateToTransport = { navController.navigate("transport") },
                                onNavigateToCamera = { navController.navigate("camera") },
                                onLogout = {
                                    userPreferences.clearSession()
                                    ApiClient.token = null
                                    navController.navigate("welcome") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onSpeak = { text -> audioAssistant.speak(text) },
                                userName = currentUserName,
                                onMicClick = { conversationalAssistant.startListeningManual() },
                                onNavigateToProfile = { navController.navigate("profile") }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                onBackClick = { navController.popBackStack() },
                                onSpeak = { text -> audioAssistant.speak(text) },
                                onProfileUpdated = { nombre ->
                                    currentUserName = nombre
                                    userPreferences.saveUser(nombre, userPreferences.getUserEmail(), true)
                                },
                                onAccountDeleted = {
                                    userPreferences.clearSession()
                                    ApiClient.token = null
                                    navController.navigate("welcome") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("transport") {
                            TransportScreen(
                                onBackClick = { navController.popBackStack() },
                                onNavigateToHome = { navController.navigate("main_menu") },
                                onNavigateToCamera = { navController.navigate("camera") },
                                onSpeak = { text -> audioAssistant.speak(text) }
                            )
                        }

                        composable("camera") {
                            CameraScreen(
                                userName = currentUserName,
                                onBackClick = { navController.popBackStack() },
                                onSpeak = { text -> audioAssistant.speak(text) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VoiceSettings.onChange = null
        audioAssistant.shutdown()
        if (::conversationalAssistant.isInitialized) {
            conversationalAssistant.stop()
        }
    }
}
