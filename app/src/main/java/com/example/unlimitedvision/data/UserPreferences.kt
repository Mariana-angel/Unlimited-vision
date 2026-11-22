package com.example.unlimitedvision.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("unlimited_vision_prefs", Context.MODE_PRIVATE)

    fun saveUser(name: String, email: String, remember: Boolean = true) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, remember)
            apply()
        }
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Usuario") ?: "Usuario"
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /** Token JWT que entrega el backend al iniciar sesión o registrarse */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** Contacto de emergencia guardado también en el celular, para que el SOS funcione sin internet */
    fun saveEmergencyContact(nombre: String?, telefono: String?) {
        prefs.edit().putString(KEY_CONTACT_NAME, nombre).putString(KEY_CONTACT_PHONE, telefono).apply()
    }

    fun getEmergencyContactName(): String? = prefs.getString(KEY_CONTACT_NAME, null)?.ifBlank { null }
    fun getEmergencyContactPhone(): String? = prefs.getString(KEY_CONTACT_PHONE, null)?.ifBlank { null }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_TOKEN)
            apply()
        }
    }

    companion object {
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_TOKEN = "key_token"
        private const val KEY_CONTACT_NAME = "key_contact_name"
        private const val KEY_CONTACT_PHONE = "key_contact_phone"
    }
}
