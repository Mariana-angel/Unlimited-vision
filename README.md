# Unlimited Vision 👁️

App Android para **personas con discapacidad visual** (y sus acompañantes) que ayuda a moverse por Bogotá: asistente de voz, transporte (TransMilenio/SITP), cámara que describe el entorno y botón SOS.

- **App:** Kotlin + Jetpack Compose (esta carpeta).
- **Backend:** NestJS + MySQL (XAMPP) + Swagger, en `C:\Users\Pablo\AndroidStudioProjects\unlimited-vision-backend\unlimited-vision-backend`.

---

## Índice

1. [Cómo ponerlo a correr](#1-cómo-ponerlo-a-correr)
2. [Cómo está organizado (arquitectura)](#2-cómo-está-organizado-arquitectura)
3. [Mapa de carpetas y archivos](#3-mapa-de-carpetas-y-archivos)
4. [El asistente de voz por dentro](#4-el-asistente-de-voz-por-dentro)
5. [Modo acompañante y velocidad de voz](#5-modo-acompañante-y-velocidad-de-voz)
6. [Conexión con el backend](#6-conexión-con-el-backend)
7. [Pantallas: qué hace cada una](#7-pantallas-qué-hace-cada-una)
8. [Ubicación, SOS y mapa](#8-ubicación-sos-y-mapa)
9. [Cámara y lectura de texto (ML Kit)](#9-cámara-y-lectura-de-texto-ml-kit)
10. [Backend: estructura, endpoints y base de datos](#10-backend-estructura-endpoints-y-base-de-datos)
11. [Recetas: cómo agregar cosas nuevas](#11-recetas-cómo-agregar-cosas-nuevas)
12. [Problemas comunes y soluciones](#12-problemas-comunes-y-soluciones)
13. [Carpetas de respaldo](#13-carpetas-de-respaldo)

---

## 1. Cómo ponerlo a correr

### Orden de encendido (siempre)

| Paso | Qué | Cómo |
|---|---|---|
| 1 | **MySQL** | Abrir XAMPP → *Start* en MySQL |
| 2 | **Backend** | En la carpeta del backend abrir `cmd` → `npm run start:dev` |
| 3 | **Swagger** (opcional) | Navegador → http://localhost:3000/api/docs |
| 4 | **App** | Android Studio → emulador **Pixel 6 API 35** → *Run ▶* |

> La primera vez del backend también hay que hacer `copy .env.example .env` y `npm install`.

### Configuración del emulador

- **Micrófono:** ⋮ → *Extended Controls* → *Microphone* → activar **Enable Host Microphone Access** (se apaga cada vez que se reinicia el emulador).
- **Ubicación:** ⋮ → *Extended Controls* → *Location* → buscar una dirección de Bogotá → **Set Location** (por defecto el emulador está en California).
- Usar **API 35**. El emulador con API 37 (versión de prueba) deja la pantalla negra.

### Google Maps

La API key va en `local.properties` (este archivo **no se sube a GitHub**):

```
MAPS_API_KEY=AIzaSy...tu_clave
```

`app/build.gradle.kts` la lee y la pone en el `AndroidManifest.xml` (`${MAPS_API_KEY}`).

---

## 2. Cómo está organizado (arquitectura)

```
┌──────────────────────────── APP ANDROID ────────────────────────────┐
│                                                                     │
│  screens/  (lo que se ve)  ◄─── MainActivity (navegación + enlaces)  │
│     │                              │                                 │
│     │ onSpeak("...")               ├─► audio/   (voz: hablar/escuchar)│
│     │                              ├─► network/ (Retrofit → backend)  │
│     │                              ├─► data/    (guardar en celular)  │
│     │                              └─► location/(GPS, SOS, compartir) │
│  components/ (botones, campos reutilizables)                         │
└──────────────────────────────────────┬──────────────────────────────┘
                                       │ HTTP + JSON (token JWT)
                                       ▼
┌────────────────── BACKEND NestJS (puerto 3000) ──────────────────┐
│  auth/  → registro, login, JWT                                    │
│  usuarios/ → perfil, preferencias, contacto de emergencia         │
│  rutas/ → rutas de cada usuario (CRUD, favoritas)                 │
│  Swagger: /api/docs                                               │
└──────────────────────────────┬────────────────────────────────────┘
                               ▼
                 MySQL (XAMPP) · base: unlimited_vision
                 tablas: usuarios, rutas
```

**Idea clave:** las pantallas **no** hablan ni llaman al backend directamente: reciben funciones (`onSpeak`, `onLoginSubmit`, etc.) que les pasa `MainActivity`. Así cada pantalla se puede probar sola y la lógica queda en un solo lugar.

---

## 3. Mapa de carpetas y archivos

Ruta base: `app/src/main/java/com/example/unlimitedvision/`

| Archivo | Para qué sirve |
|---|---|
| **`MainActivity.kt`** | Punto de entrada. Crea la voz, pide permisos, decide la pantalla inicial (bienvenida o menú si ya hay sesión), define **todas las rutas de navegación** (`welcome`, `login`, `register`, `main_menu`, `transport`, `camera`, `profile`) y conecta las pantallas con el backend y el asistente. |
| **`audio/AudioAssistant.kt`** | **La voz que habla** (Text-To-Speech). Función `speak(texto, onDone)`. |
| **`audio/VoiceCommandListener.kt`** | **El oído**: reconocimiento de voz de Android (`SpeechRecognizer`). Devuelve lo que la persona dijo. |
| **`audio/ConversationalAssistant.kt`** | **El cerebro de la conversación**: máquina de estados que guía registro, login y comandos ("transporte", "cámara", "SOS"…). |
| **`audio/VoiceSettings.kt`** | Interruptor global de voz (**modo acompañante**) y **velocidad de la voz**. Se guarda en el celular. |
| **`components/CommonComponents.kt`** | Encabezado (`AppHeader`), barra inferior (`AppBottomNavBar`), botón de voz on/off (`VoiceToggleButton`) y franja roja de voz apagada (`VoiceOffBanner`). |
| **`components/AuthComponents.kt`** | Piezas reutilizables: campo accesible con botón 🔊 (`AccessibleTextField`), medidor de contraseña, botones grandes, micrófono que pulsa, tarjetas de opción, vibración (`rememberHaptic`). |
| **`data/UserPreferences.kt`** | Lo que se guarda **en el celular**: nombre, correo, sesión, **token JWT**, contacto de emergencia. |
| **`location/LocationHelper.kt`** | GPS + dirección aproximada, **compartir ubicación**, **SMS de emergencia**, abrir marcador de teléfono. |
| **`network/ApiClient.kt`** | Configuración de Retrofit. Aquí está la **URL del backend** (`BASE_URL`) y el interceptor que agrega el token a cada petición. |
| **`network/UnlimitedVisionApi.kt`** | Lista de **endpoints** del backend (los mismos de Swagger). |
| **`network/ApiModels.kt`** | Clases de datos que se envían y reciben (JSON). |
| **`network/AuthRepository.kt`** | Registro y login con manejo de errores (convierte respuestas del backend en mensajes entendibles). |
| **`screens/WelcomeScreen.kt`** | Bienvenida. |
| **`screens/LoginScreen.kt`** | Inicio de sesión. |
| **`screens/RegisterScreen.kt`** | Registro. |
| **`screens/MainMenuScreen.kt`** | Menú principal (ubicación, accesos rápidos, SOS). |
| **`screens/TransportScreen.kt`** | Transporte, rutas, paradas y mapa. |
| **`screens/CameraScreen.kt`** | Cámara de asistencia y lectura de texto. |
| **`screens/ProfileScreen.kt`** | Perfil y ajustes. |
| **`theme/Theme.kt`** | Colores de la app (teal, oliva, naranja…). |

Otros archivos importantes:

| Archivo | Para qué sirve |
|---|---|
| `app/src/main/AndroidManifest.xml` | Permisos (internet, ubicación, cámara, micrófono), `usesCleartextTraffic` (para hablar con el backend por `http`) y la key de Maps. |
| `app/build.gradle.kts` | Dependencias (Compose, Retrofit, Maps, CameraX, ML Kit) y lectura de `MAPS_API_KEY`. |
| `gradle/libs.versions.toml` | Versiones de las librerías. |
| `local.properties` | Ruta del SDK y **API key de Maps** (privado). |

---

## 4. El asistente de voz por dentro

Son **3 piezas** que trabajan en cadena:

```
 AudioAssistant          VoiceCommandListener        ConversationalAssistant
  (BOCA: habla)    ──►     (OÍDO: escucha)     ──►    (CEREBRO: decide qué hacer)
        ▲                                                       │
        └──────────────────── responde ◄────────────────────────┘
```

### 4.1 El ciclo "hablar → escuchar → responder"

Todo pasa en `audio/ConversationalAssistant.kt`, en la función **`speakAndListen(texto)`**:

1. **Habla:** llama a `audioAssistant.speak(texto) { ... }`.
2. **Espera a que termine de hablar:** `AudioAssistant` tiene un `UtteranceProgressListener`; cuando la voz termina se ejecuta el `onDone`.
3. **Empieza a escuchar:** 400 ms después llama a `voiceListener.startListening()` (para no escucharse a sí misma).
4. **Recibe lo que dijo la persona:** `VoiceCommandListener.onResults()` toma el texto reconocido y llama a `processUserInput(texto)`.
5. **Decide:** `processUserInput` mira en qué **paso** está la conversación (`currentStep`) y responde con otro `speakAndListen(...)` → vuelve al paso 1.

> **¿Por qué se escucha cuando uno responde?** Porque cada respuesta del asistente termina en `speakAndListen`, que al acabar de hablar **vuelve a abrir el micrófono** automáticamente.

### 4.2 Los pasos de la conversación (`AssistantStep`)

Definidos al inicio de `ConversationalAssistant.kt`:

| Paso | Qué espera escuchar | Qué hace |
|---|---|---|
| `GREETING` | "iniciar sesión" / "registrarme" | Navega a login o registro |
| `REGISTER_NAME` → `REGISTER_NAME_CONFIRM` | Nombre → "sí"/"no" | Llena el campo nombre y confirma |
| `REGISTER_LASTNAME` → `..._CONFIRM` | Apellido → "sí"/"no" | |
| `REGISTER_EMAIL` → `..._CONFIRM` | Correo (convierte "arroba" → @, "punto com" → .com) | `parseEmailFromSpeech` |
| `REGISTER_PASSWORD` | Contraseña ("1 2 3 4 5 6" → "123456") | `parsePasswordFromSpeech`; si tiene < 6 la vuelve a pedir |
| `LOGIN_EMAIL` → `LOGIN_PASSWORD` | Correo y contraseña | |
| `IDLE` | — | Esperando respuesta del backend (no escucha) |
| `MAIN_CONVERSATIONAL` | Comandos | `processGlobalCommands` |

**Comandos globales** (`processGlobalCommands`): *transporte/bus/ruta*, *cámara/foto/ver*, *menú/ubicación/inicio*, *perfil/ajustes*, *SOS/emergencia/ayuda*, *cerrar/salir*.
En cualquier paso de registro o login, decir **"cancelar"**, **"cerrar"** o **"volver"** regresa al inicio.

### 4.3 Cómo se llenan los campos mientras uno habla

`ConversationalAssistant` recibe funciones como `onUpdateNameField`, `onUpdateEmailField`… (`MainActivity` las conecta a variables `voiceNombreState`, `voiceEmailState`, `voicePasswordState`). Las pantallas de Login/Registro reciben esos valores y los copian a sus campos con `LaunchedEffect`. Incluso con resultados parciales (`updateFieldsPartial`) el texto aparece mientras la persona habla.

### 4.4 Qué pasa al terminar registro/login por voz

1. `ConversationalAssistant` llama a `onRegisterCompleted(nombre, correo, clave)` o `onLoginCompleted(correo, clave)`.
2. `MainActivity` envía los datos al backend con `AuthRepository`.
3. Según la respuesta, `MainActivity` llama de vuelta:
   - `onAuthSuccess(nombre, esRegistro)` → saluda y queda escuchando comandos.
   - `onRegisterFailed(mensaje, correoYaExiste)` → si el correo existe, pide otro.
   - `onLoginFailed(mensaje)` → explica el error y vuelve a pedir el correo.

### 4.5 El saludo inicial

En `MainActivity.kt`, `tryStartWelcomeGreeting()` lanza `startWelcomeFlow()` **solo cuando** la voz ya está lista (`audioAssistant.whenReady`) **y** hay permiso de micrófono. Se llama al abrir la app y cuando el usuario acepta el permiso.

### 4.6 Si no entiende

`VoiceCommandListener.onError()` avisa a `handleSpeechError()`: dice *"No te escuché bien, ¿me lo repites?"* hasta 2 veces; a la tercera pide tocar el botón del micrófono (evita ciclos infinitos). El reconocimiento termina tras ~2 s de silencio (`EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS`).

### 4.7 Las pantallas también hablan

Cada pantalla recibe `onSpeak: (String) -> Unit`. `MainActivity` lo conecta a `audioAssistant.speak(...)`. Los botones 🔊 de cada campo, tarjetas y errores usan `onSpeak`.

---

## 5. Modo acompañante y velocidad de voz

Archivo: `audio/VoiceSettings.kt` (objeto global, se guarda en el celular).

- **`VoiceSettings.enabled`**: si es `false`, `AudioAssistant.speak()` no habla y **no** encadena la escucha. Botón 🗣️ en el encabezado de todas las pantallas (`VoiceToggleButton`) y franja roja *"Modo acompañante"* (`VoiceOffBanner`).
- Al apagarla, `MainActivity` (en `VoiceSettings.onChange`) corta lo que esté diciendo (`stopSpeaking`) y apaga el micrófono (`conversationalAssistant.silence()`).
- **`VoiceSettings.speechRate`**: velocidad de 0.5x a 2.0x. Se cambia en **Perfil** y `AudioAssistant` la aplica al instante.

---

## 6. Conexión con el backend

- **URL:** `network/ApiClient.kt` elige sola la dirección (`BASE_URL`):
  - **Emulador** → `http://10.0.2.2:3000/` (`10.0.2.2` = el "localhost" de tu PC visto desde el emulador).
  - **Celular por cable USB** → `http://localhost:3000/`. Antes hay que ejecutar en la terminal de Android Studio: `adb reverse tcp:3000 tcp:3000` (se repite cada vez que se conecta el cable).
  - **Celular por Wi-Fi** → poner la IP del PC en `local.properties` (`BACKEND_HOST=192.168.x.x`), estar en la misma red y permitir el puerto 3000 en el Firewall de Windows.
- **Token JWT:** al registrarse o iniciar sesión, `MainActivity.guardarSesion()` guarda el `accessToken` en `UserPreferences` y en `ApiClient.token`. El interceptor de `ApiClient` lo agrega solo a cada petición (`Authorization: Bearer ...`).
- Al abrir la app, si hay token guardado, va directo al menú principal.
- Para ver las peticiones: **Logcat** filtrando por `okhttp`.

---

## 7. Pantallas: qué hace cada una

| Pantalla | Archivo | Lo principal |
|---|---|---|
| Bienvenida | `WelcomeScreen.kt` | Logo animado, 4 tarjetas de funciones (se leen al tocarlas), botones Ingresar / Crear cuenta, micrófono grande. |
| Login | `LoginScreen.kt` | Validación en vivo, "Mantener sesión", "¿Olvidaste la contraseña?", "Ingresar con voz". |
| Registro | `RegisterScreen.kt` | Nombre, correo, teléfono, nivel de visión, contraseña con medidor de seguridad, **confirmar contraseña**, términos, barra de progreso (x/5), botón fijo abajo, salta solo al campo con error. |
| Menú principal | `MainMenuScreen.kt` | Saludo según la hora, ubicación real, compartir / llamar 123, accesos rápidos, **SOS (mantener presionado)**, avatar → perfil, cerrar sesión con confirmación. |
| Transporte | `TransportScreen.kt` | "¿A dónde quieres ir?", medio (TransMilenio, SITP, caminando, taxi, bici), **Iniciar viaje** (guarda la ruta en el backend), mis rutas con ⭐ favoritas y 🗑️ eliminar, paradas cercanas, mapa de Google. |
| Cámara | `CameraScreen.kt` | Modos Explorar / **Leer texto (real)** / Billetes, botón Describir, linterna, cuadrícula, girar cámara. |
| Perfil | `ProfileScreen.kt` | Datos, nivel de visión, **contacto de emergencia**, voz on/off, **velocidad de voz**, eliminar cuenta. |

> Las **paradas** de Transporte y las detecciones de **Explorar/Billetes** en la cámara son **datos de ejemplo** (marcados como DEMO). La lectura de texto sí es real.

---

## 8. Ubicación, SOS y mapa

Archivo: `location/LocationHelper.kt`

- `obtenerUbicacion()` → GPS (Fused Location) + dirección aproximada con `Geocoder`.
- **SOS** (`MainMenuScreen.activarSOS`):
  - Si hay **contacto de emergencia** guardado → abre un **SMS** a ese contacto con el enlace de Google Maps de tu ubicación (`enviarSmsEmergencia`).
  - Si no → abre el menú de compartir (WhatsApp, etc.) con la ubicación.
  - Se activa **manteniendo presionado** para evitar alertas por accidente.
- **Mapa:** `TransportScreen.kt` usa `GoogleMap` (maps-compose). Si no hay key, muestra un aviso en lugar del recuadro gris.

---

## 9. Cámara y lectura de texto (ML Kit)

Archivo: `screens/CameraScreen.kt`

- **CameraX:** `Preview` (lo que se ve) + `ImageAnalysis` (analiza fotogramas). Se vuelve a conectar al cambiar de cámara (`LaunchedEffect(lensFacing)`).
- **ML Kit Text Recognition** (`crearAnalizadorTexto`): en modo *Leer texto* analiza un fotograma cada 1,2 s y guarda el texto en `textoDetectado`. Funciona **sin internet**.
- **Describir** lee en voz alta lo detectado. **Linterna** usa `camera.cameraControl.enableTorch()`.

---

## 10. Backend: estructura, endpoints y base de datos

Carpeta: `unlimited-vision-backend/unlimited-vision-backend/`

```
src/
├── main.ts                  # Arranque, validación, CORS y Swagger (/api/docs)
├── app.module.ts            # Conexión a MySQL (synchronize: crea/actualiza tablas solo)
├── common/
│   ├── crear-base-datos.ts  # Crea la base "unlimited_vision" si no existe
│   └── usuario-actual.decorator.ts  # Saca el usuario del token
├── auth/                    # registro, login, JWT (jwt.strategy, jwt-auth.guard)
├── usuarios/                # entidad Usuario + perfil (/usuarios/me)
└── rutas/                   # entidad Ruta + CRUD (/rutas)
.env                         # puerto, datos de MySQL y JWT_SECRET
database/unlimited_vision.sql  # modelo de la base (referencia)
```

### Endpoints

| Método | Ruta | Token | Para qué |
|---|---|---|---|
| POST | `/auth/registro` | ❌ | Crear cuenta → devuelve `accessToken` |
| POST | `/auth/login` | ❌ | Iniciar sesión → devuelve `accessToken` |
| GET | `/usuarios/me` | ✅ | Ver mi perfil |
| PATCH | `/usuarios/me` | ✅ | Editar perfil, voz, contacto de emergencia |
| DELETE | `/usuarios/me` | ✅ | Eliminar cuenta (y sus rutas) |
| POST | `/rutas` | ✅ | Guardar una ruta |
| GET | `/rutas?favoritas=true` | ✅ | Listar mis rutas |
| GET | `/rutas/:id` | ✅ | Ver una ruta |
| PATCH | `/rutas/:id` | ✅ | Editar (ej. marcar favorita) |
| DELETE | `/rutas/:id` | ✅ | Eliminar ruta |

### Base de datos (MySQL)

```
usuarios                                  rutas
─────────────────────────                 ─────────────────────────
id (PK)                                    id (PK)
nombre, apellido                           origen, destino
email (único)                              origen_lat/lng, destino_lat/lng
password_hash (bcrypt, nunca se devuelve)  medio_transporte
telefono                                   distancia_metros, duracion_minutos
nivel_vision                               favorita
velocidad_voz, asistente_voz_activo        usuario_id (FK → usuarios.id, ON DELETE CASCADE)
contacto_nombre, contacto_telefono         creado_en, actualizado_en
creado_en, actualizado_en
```

Relación: **un usuario tiene muchas rutas** (1:N). Cada usuario solo puede ver y editar **sus** rutas.

---

## 11. Recetas: cómo agregar cosas nuevas

### Agregar un comando de voz (ej. "hora")
En `audio/ConversationalAssistant.kt`, dentro de `processGlobalCommands`, agregar una rama antes del `else`:
```kotlin
text.contains("hora") -> {
    val hora = java.text.SimpleDateFormat("h:mm a", java.util.Locale("es", "CO")).format(java.util.Date())
    speakAndListen("Son las $hora.")
}
```

### Agregar una pantalla nueva
1. Crear `screens/MiPantalla.kt` con `@Composable fun MiPantalla(onBackClick: () -> Unit, onSpeak: (String) -> Unit)`.
2. En `MainActivity.kt`, dentro del `NavHost`, agregar `composable("mi_pantalla") { MiPantalla(...) }`.
3. Navegar con `navController.navigate("mi_pantalla")`.

### Agregar un endpoint nuevo
1. **Backend:** agregar el método en el controller (`@Get`, `@Post`…) con decoradores de Swagger.
2. **App:** declararlo en `network/UnlimitedVisionApi.kt` y crear sus clases en `network/ApiModels.kt`.

### Agregar un campo al usuario
1. Backend: `usuarios/usuario.entity.ts` (`@Column`) y `dto/actualizar-usuario.dto.ts`.
2. App: `network/ApiModels.kt` (`UsuarioDto` y `ActualizarUsuarioRequest`) y la pantalla de Perfil.

---

## 12. Problemas comunes y soluciones

| Problema | Causa | Solución |
|---|---|---|
| MySQL de XAMPP no arranca | Puerto 3306 ocupado por otro MySQL | `services.msc` → detener **MySQL80** |
| "No me pude conectar con el servidor" | Backend apagado | `npm run start:dev` |
| Pantalla negra en la app | Emulador API 37 | Usar emulador **API 35** |
| El asistente habla pero no escucha | Micrófono del emulador apagado | Extended Controls → Microphone → activar |
| "Unable to delete directory…" al compilar | Proyecto dentro de OneDrive | El proyecto ya está en `AndroidStudioProjects` (fuera de OneDrive) |
| Mapa en California | Ubicación por defecto del emulador | Extended Controls → Location → Bogotá |
| Mapa en blanco / "Mapa no configurado" | Falta `MAPS_API_KEY` | Ponerla en `local.properties` y Sync |
| 401 en Swagger o en la app | Token vencido (dura 7 días) | Volver a iniciar sesión |
| Emulador muy lento | PC con poca memoria libre | Cerrar BlueStacks/MSI App Player u otros programas pesados, o usar el celular por USB |

---

## 13. Carpetas de respaldo

Antes de cada cambio grande se guardó una copia de los archivos originales (no se compilan, se pueden borrar cuando todo funcione):

| Carpeta | Contiene la versión antes de… |
|---|---|
| `_respaldo_antes_backend` | conectar la app al backend |
| `_respaldo_antes_voz` | arreglar el reconocimiento de voz |
| `_respaldo_antes_front` | rediseñar bienvenida, login y registro |
| `_respaldo_antes_modo_acompanante` | el modo acompañante |
| `_respaldo_antes_pantallas_internas` | rediseñar menú, transporte y cámara |
| `_respaldo_antes_perfil` | perfil, favoritas, ML Kit y Maps |
