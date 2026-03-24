package com.example.controlacademico.prefs

import android.content.Context

class Prefs(val context: Context) {

    // Sesion — se borra al cerrar sesión
    private val session = context.getSharedPreferences("CA_Session", 0)

    // Dispositivo — NUNCA se borra, es del dispositivo
    private val device = context.getSharedPreferences("CA_Device", 0)

    fun guardarSesion(email: String, uid: String, rol: String, nombre: String = "") {
        session.edit().apply {
            putString("email", email)
            putString("uid", uid)
            putString("rol", rol)
            putString("nombre", nombre)
            apply()
        }
    }

    fun getEmail(): String = session.getString("email", "") ?: ""
    fun getUid(): String = session.getString("uid", "") ?: ""
    fun getRol(): String = session.getString("rol", "") ?: ""
    fun getNombre(): String = session.getString("nombre", "") ?: ""
    fun haySesion(): Boolean = getEmail().isNotEmpty()

    fun cerrarSesion() {
        // Solo borra la sesión, NO el tema
        session.edit().clear().apply()
    }

    // Tema — guardado en el dispositivo, independiente de sesión y rol
    fun setTemaOscuro(oscuro: Boolean) {
        device.edit().putBoolean("tema_oscuro", oscuro).apply()
    }

    fun isTemaOscuro(): Boolean = device.getBoolean("tema_oscuro", true)
}