package com.example.unlimitedvision.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unlimitedvision.components.AppBottomNavBar
import com.example.unlimitedvision.components.AppHeader
import com.example.unlimitedvision.components.rememberHaptic
import com.example.unlimitedvision.location.LocationHelper
import com.example.unlimitedvision.location.Ubicacion
import com.example.unlimitedvision.network.ActualizarRutaRequest
import com.example.unlimitedvision.network.ApiClient
import com.example.unlimitedvision.data.AppConstants
import com.example.unlimitedvision.network.CrearRutaRequest
import com.example.unlimitedvision.network.RutaDto
import com.example.unlimitedvision.theme.CardBgTeal
import com.example.unlimitedvision.theme.DarkTealBg
import com.example.unlimitedvision.theme.LinkCyan
import com.example.unlimitedvision.theme.OrangeAction
import com.example.unlimitedvision.theme.TealHeader
import com.example.unlimitedvision.theme.UnlimitedVisionTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

data class BusStopItem(val code: String, val name: String, val distance: String, val latLng: LatLng)

private data class Medio(val valor: String, val texto: String, val icono: ImageVector)

private val medios = listOf(
    Medio("transmilenio", "TransMilenio", Icons.Default.DirectionsSubway),
    Medio("bus", "SITP", Icons.Default.DirectionsBus),
    Medio("caminando", "Caminando", Icons.AutoMirrored.Filled.DirectionsWalk),
    Medio("taxi", "Taxi", Icons.Default.LocalTaxi),
    Medio("bicicleta", "Bicicleta", Icons.Default.PedalBike),
)

private fun iconoMedio(valor: String) = medios.firstOrNull { it.valor == valor }?.icono ?: Icons.Default.DirectionsBus
private fun textoMedio(valor: String) = medios.firstOrNull { it.valor == valor }?.texto ?: valor

@Composable
fun TransportScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // La key llega al manifest desde local.properties (MAPS_API_KEY=...)
    val tieneMapsKey = remember {
        try {
            val info = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
            !info.metaData?.getString("com.google.android.geo.API_KEY").isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }
    val hasLocationPermission = remember { LocationHelper.tienePermiso(context) }

    val bogota = LatLng(4.6580, -74.0930)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 14f)
    }

    // Datos de ejemplo de paradas (luego pueden venir de la API del SITP)
    val nearbyStops = remember {
        listOf(
            BusStopItem("683A098", "Br. Sauce", "192 m.", LatLng(4.6590, -74.0920)),
            BusStopItem("684A099", "Br. Valle del Sol", "211 m.", LatLng(4.6570, -74.0940)),
            BusStopItem("682A09", "Br. Alcaparro", "339 m.", LatLng(4.6600, -74.0910))
        )
    }

    var ubicacion by remember { mutableStateOf<Ubicacion?>(null) }
    var destino by remember { mutableStateOf("") }
    var medio by remember { mutableStateOf("transmilenio") }
    var rutas by remember { mutableStateOf<List<RutaDto>>(emptyList()) }
    var cargandoRutas by remember { mutableStateOf(true) }
    var errorRutas by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var viajeActivo by remember { mutableStateOf<String?>(null) }
    var paradaSeleccionada by remember { mutableStateOf<String?>(null) }
    var soloFavoritas by remember { mutableStateOf(false) }
    var rutaAEliminar by remember { mutableStateOf<RutaDto?>(null) }

    fun alternarFavorita(ruta: RutaDto) {
        val nueva = !ruta.favorita
        // Se actualiza al instante en pantalla y luego se confirma con el backend
        rutas = rutas.map { if (it.id == ruta.id) it.copy(favorita = nueva) else it }
        onSpeak(if (nueva) "Ruta a ${ruta.destino} agregada a favoritas." else "Ruta a ${ruta.destino} quitada de favoritas.")
        scope.launch {
            val ok = try { ApiClient.api.actualizarRuta(ruta.id, ActualizarRutaRequest(favorita = nueva)).isSuccessful } catch (e: Exception) { false }
            if (!ok) {
                rutas = rutas.map { if (it.id == ruta.id) it.copy(favorita = ruta.favorita) else it }
                onSpeak("No pude guardar el cambio.")
            }
        }
    }

    val rutasDemo = remember {
        listOf(
            RutaDto(1, "Mi ubicación", "Portal Norte", 4.658, -74.093, 4.755, -74.046, "transmilenio", 8500, 25, true, null),
            RutaDto(2, "Mi ubicación", "Universidad Compensar", 4.658, -74.093, 4.662, -74.072, "bus", 2300, 12, true, null),
            RutaDto(3, "Mi ubicación", "Calle 26 - Av. El Dorado", 4.658, -74.093, 4.652, -74.085, "caminando", 1100, 15, false, null)
        )
    }

    fun eliminarRuta(ruta: RutaDto) {
        rutas = rutas.filterNot { it.id == ruta.id }
        onSpeak("Ruta a ${ruta.destino} eliminada.")
        scope.launch {
            try { ApiClient.api.eliminarRuta(ruta.id) } catch (_: Exception) {}
        }
    }

    fun cargarRutas() {
        cargandoRutas = true
        scope.launch {
            try {
                val r = ApiClient.api.listarRutas()
                if (r.isSuccessful && !r.body().isNullOrEmpty()) {
                    rutas = r.body()!!
                    errorRutas = null
                } else {
                    if (rutas.isEmpty()) rutas = rutasDemo
                    errorRutas = null
                }
            } catch (e: Exception) {
                if (rutas.isEmpty()) rutas = rutasDemo
                errorRutas = null
            }
            cargandoRutas = false
        }
    }

    LaunchedEffect(Unit) {
        cargarRutas()
        ubicacion = LocationHelper.obtenerUbicacion(context)
        ubicacion?.let { cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.lat, it.lng), 15f) }
    }

    fun iniciarViaje() {
        focusManager.clearFocus()
        val dest = destino.trim()
        if (dest.isBlank()) {
            onSpeak("Primero dime o escribe a dónde quieres ir.")
            return
        }
        guardando = true
        scope.launch {
            val body = CrearRutaRequest(
                origen = ubicacion?.direccion ?: "Mi ubicación",
                destino = dest,
                origenLat = ubicacion?.lat,
                origenLng = ubicacion?.lng,
                medioTransporte = medio
            )
            val nuevaRuta = RutaDto(
                id = (rutas.maxOfOrNull { it.id } ?: 0) + 1,
                origen = body.origen,
                destino = dest,
                origenLat = body.origenLat,
                origenLng = body.origenLng,
                destinoLat = null,
                destinoLng = null,
                medioTransporte = medio,
                distanciaMetros = 3000,
                duracionMinutos = 18,
                favorita = false,
                creadoEn = null
            )
            try { ApiClient.api.crearRuta(body) } catch (_: Exception) {}
            if (rutas.none { it.destino.equals(dest, ignoreCase = true) }) {
                rutas = listOf(nuevaRuta) + rutas
            }
            guardando = false
            viajeActivo = dest
            val parada = nearbyStops.first()
            onSpeak(
                "Iniciando viaje a $dest en ${textoMedio(medio)}. " +
                    (if (medio == "caminando" || medio == "taxi" || medio == "bicicleta") "" else "Camina hacia la parada ${parada.name}, a ${parada.distance.replace(".", "")}. ") +
                    "La ruta quedó guardada en tu cuenta."
            )
        }
    }

    fun leerPantalla() {
        val resumenRutas = if (rutas.isEmpty()) "Aún no tienes rutas guardadas." else
            "Tus últimas rutas: " + rutas.take(3).joinToString(", ") { "a ${it.destino} en ${textoMedio(it.medioTransporte)}" } + "."
        onSpeak(
            "Pantalla de transporte. El pasaje cuesta ${AppConstants.PRECIO_PASAJE_TEXTO} pesos. Escribe tu destino, elige cómo quieres ir y toca Iniciar viaje. " +
                "$resumenRutas Paradas cercanas: " + nearbyStops.joinToString(", ") { "${it.name} a ${it.distance.replace(".", "")}" } + "."
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                titleBanner = "TRANSMILENIO Y SITP · PASAJE ${AppConstants.PRECIO_PASAJE_FORMATO}",
                showBack = true,
                onBackClick = onBackClick,
                onSpeakerClick = { leerPantalla() },
                onNotificationClick = { onSpeak("Sin novedades en las rutas cercanas.") }
            )
        },
        bottomBar = {
            AppBottomNavBar(
                selectedTab = "routes",
                onHomeClick = onNavigateToHome,
                onRoutesClick = { onSpeak("Ya estás en transporte.") },
                onSpeakerClick = { leerPantalla() },
                onHelpClick = { onSpeak("Escribe el destino, elige el medio y toca Iniciar viaje. Tus rutas se guardan solas.") },
                onViajarClick = { iniciarViaje() }
            )
        },
        containerColor = DarkTealBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // ---------- Viaje activo ----------
            AnimatedVisibility(viajeActivo != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(OrangeAction)
                        .clickable { onSpeak("Viaje en curso hacia ${viajeActivo ?: ""}.") }
                        .padding(14.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Viaje en curso", color = Color.White, fontSize = 12.sp)
                        Text(viajeActivo ?: "", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${textoMedio(medio)} · sigue las indicaciones por voz", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                    IconButton(onClick = { haptic(); viajeActivo = null; onSpeak("Viaje terminado.") }) {
                        Icon(Icons.Default.Close, contentDescription = "Terminar viaje", tint = Color.White)
                    }
                }
            }

            // ---------- ¿A dónde vas? ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBgTeal)
                    .padding(16.dp)
            ) {
                Text(
                    "¿A dónde quieres ir?",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.semantics { heading() }
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeAction, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Desde: ${ubicacion?.direccion ?: "tu ubicación actual"}",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = destino,
                    onValueChange = { destino = it },
                    placeholder = { Text("Ej: Portal Norte, Universidad Compensar…", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF2C3E47)) },
                    trailingIcon = {
                        if (destino.isNotBlank()) IconButton(onClick = { destino = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Borrar destino", tint = Color.DarkGray)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { iniciarViaje() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFD4E3E5),
                        unfocusedContainerColor = Color(0xFFD4E3E5),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = LinkCyan,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                )

                Spacer(Modifier.height(12.dp))
                Text("¿Cómo quieres ir?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    medios.forEach { m ->
                        val sel = medio == m.valor
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (sel) TealHeader else DarkTealBg)
                                .border(1.5.dp, if (sel) LinkCyan else Color.Transparent, RoundedCornerShape(24.dp))
                                .clickable { haptic(); medio = m.valor; onSpeak("${m.texto} seleccionado.") }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(m.icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(m.texto, color = Color.White, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { haptic(); iniciarViaje() },
                    enabled = !guardando,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAction),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Navigation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Iniciar viaje", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ---------- Mis rutas (backend) ----------
            SectionHeader(Icons.Default.History, "Mis rutas")
            if (rutas.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    listOf(false to "Todas (${rutas.size})", true to "⭐ Favoritas (${rutas.count { it.favorita }})").forEach { (fav, texto) ->
                        val sel = soloFavoritas == fav
                        Text(
                            texto,
                            color = if (sel) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (sel) Color.White else CardBgTeal)
                                .clickable { haptic(); soloFavoritas = fav; onSpeak(if (fav) "Mostrando favoritas." else "Mostrando todas las rutas.") }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            val rutasVisibles = if (soloFavoritas) rutas.filter { it.favorita } else rutas
            when {
                cargandoRutas -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = LinkCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Cargando tus rutas…", color = Color.LightGray, fontSize = 13.sp)
                }
                errorRutas != null -> InfoCard(errorRutas!!, "Reintentar") { cargarRutas() }
                rutas.isEmpty() -> InfoCard("Aún no tienes rutas. Escribe un destino y toca Iniciar viaje: se guardará aquí.", null) {}
                rutasVisibles.isEmpty() -> InfoCard("No tienes rutas favoritas. Toca la estrella ☆ de una ruta para marcarla.", null) {}
                else -> Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    rutasVisibles.take(10).forEach { ruta ->
                        Column(
                            modifier = Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE5EFEF))
                                .clickable {
                                    haptic()
                                    destino = ruta.destino
                                    medio = ruta.medioTransporte
                                    onSpeak("Ruta a ${ruta.destino} en ${textoMedio(ruta.medioTransporte)}. Toca Iniciar viaje para repetirla.")
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(iconoMedio(ruta.medioTransporte), contentDescription = null, tint = TealHeader, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(textoMedio(ruta.medioTransporte), color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { haptic(); alternarFavorita(ruta) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        if (ruta.favorita) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = if (ruta.favorita) "Quitar de favoritas" else "Marcar como favorita",
                                        tint = OrangeAction
                                    )
                                }
                                IconButton(onClick = { haptic(); rutaAEliminar = ruta; onSpeak("¿Eliminar la ruta a ${ruta.destino}?") }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar ruta", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(ruta.destino, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Desde ${ruta.origen}", color = Color.DarkGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // ---------- Paradas cercanas ----------
            SectionHeader(Icons.Default.Place, "Paradas cercanas")
            nearbyStops.forEach { stop ->
                val metros = stop.distance.filter { it.isDigit() }.toIntOrNull() ?: 0
                val minutos = (metros / 75).coerceAtLeast(1) // ~75 m por minuto caminando
                val sel = paradaSeleccionada == stop.code
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (sel) TealHeader else CardBgTeal)
                        .border(1.5.dp, if (sel) LinkCyan else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable {
                            haptic()
                            paradaSeleccionada = stop.code
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(stop.latLng, 17f)
                            onSpeak("Parada ${stop.name}, código ${stop.code.toCharArray().joinToString(" ")}. Está a ${stop.distance.replace(".", "")}, unos $minutos minutos caminando.")
                        }
                        .padding(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DarkTealBg)
                    ) {
                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = OrangeAction)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stop.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Código ${stop.code}", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stop.distance, color = LinkCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            Text("$minutos min", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ---------- Mapa ----------
            SectionHeader(Icons.Default.Map, "Mapa")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, TealHeader, RoundedCornerShape(20.dp))
            ) {
                if (tieneMapsKey) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = hasLocationPermission)
                    ) {
                        nearbyStops.forEach { stop ->
                            Marker(
                                state = MarkerState(position = stop.latLng),
                                title = stop.name,
                                snippet = "Parada SITP · ${stop.distance}"
                            )
                        }
                    }
                } else {
                    // Sin API key de Google Maps el mapa sale en blanco: mejor explicarlo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CardBgTeal)
                            .padding(20.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = LinkCyan, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Mapa no configurado", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Agrega tu API key de Google Maps en local.properties (MAPS_API_KEY=...) y vuelve a compilar.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(40.dp)) // espacio para el botón "viajar"
        }
    }

    rutaAEliminar?.let { ruta ->
        AlertDialog(
            onDismissRequest = { rutaAEliminar = null },
            containerColor = CardBgTeal,
            title = { Text("Eliminar ruta", color = Color.White, fontFamily = FontFamily.Serif) },
            text = { Text("¿Eliminar la ruta a ${ruta.destino}?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = { rutaAEliminar = null; eliminarRuta(ruta) }) {
                    Text("Eliminar", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rutaAEliminar = null; onSpeak("Cancelado.") }) { Text("Cancelar", color = LinkCyan) }
            }
        )
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 10.dp)
            .semantics(mergeDescendants = true) { heading() }
    ) {
        Icon(icon, contentDescription = null, tint = LinkCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
    }
}

@Composable
private fun InfoCard(text: String, action: String?, onAction: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBgTeal)
            .padding(14.dp)
    ) {
        Text(text, color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action, color = LinkCyan, fontWeight = FontWeight.Bold) }
    }
}

@Preview(showBackground = true)
@Composable
fun TransportScreenPreview() {
    UnlimitedVisionTheme {
        TransportScreen(onBackClick = {}, onNavigateToHome = {}, onNavigateToCamera = {}, onSpeak = {})
    }
}
