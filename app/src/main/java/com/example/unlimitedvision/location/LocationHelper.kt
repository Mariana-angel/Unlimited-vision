package com.example.unlimitedvision.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class Ubicacion(val lat: Double, val lng: Double, val direccion: String?)

object LocationHelper {

    fun tienePermiso(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Ubicación actual del GPS (o la última conocida) + dirección aproximada. null si no hay permiso o GPS */
    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacion(context: Context): Ubicacion? {
        if (!tienePermiso(context)) return null
        val cliente = LocationServices.getFusedLocationProviderClient(context)

        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            cliente.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        if (cont.isActive) cont.resume(loc)
                    } else {
                        cliente.lastLocation
                            .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    }
                }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        } ?: return null

        val direccion = withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.forLanguageTag("es-CO"))
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                    ?.let { a ->
                        listOfNotNull(a.thoroughfare?.let { calle -> "$calle ${a.subThoroughfare ?: ""}".trim() }, a.subLocality ?: a.locality)
                            .joinToString(", ")
                            .ifBlank { null }
                    }
            } catch (e: Exception) {
                null
            }
        }
        return Ubicacion(location.latitude, location.longitude, direccion)
    }

    /** Abre el menú de compartir con un enlace de Google Maps a la ubicación */
    fun compartirUbicacion(context: Context, ubicacion: Ubicacion?, mensajeExtra: String = "") {
        val texto = if (ubicacion != null) {
            "$mensajeExtra Mi ubicación: https://maps.google.com/?q=${ubicacion.lat},${ubicacion.lng}".trim()
        } else {
            "$mensajeExtra No pude obtener mi ubicación exacta, por favor llámame.".trim()
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir ubicación").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Abre la app de mensajes con el SMS de emergencia listo para enviar al contacto */
    fun enviarSmsEmergencia(context: Context, telefono: String, ubicacion: Ubicacion?) {
        val enlace = ubicacion?.let { "https://maps.google.com/?q=${it.lat},${it.lng}" } ?: "(ubicación no disponible)"
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$telefono")).apply {
            putExtra("sms_body", "🆘 Necesito ayuda. Mi ubicación: $enlace — enviado desde Unlimited Vision")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            compartirUbicacion(context, ubicacion, "🆘 Necesito ayuda.")
        }
    }

    /** Abre el marcador del teléfono (no llama solo: la persona confirma) */
    fun marcar(context: Context, numero: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
