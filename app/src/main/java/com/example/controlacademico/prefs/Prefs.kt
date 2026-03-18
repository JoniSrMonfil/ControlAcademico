package com.example.controlacademico.prefs

import android.content.Context

class Prefs(val context: Context) {
    private val storage = context.getSharedPreferences("ControlAcademico", 0)

    fun guardarSesion(email: String, uid: String, rol: String, nombre: String = "") {
        storage.edit().apply {
            putString("email", email)
            putString("uid", uid)
            putString("rol", rol)
            putString("nombre", nombre)
            apply()
        }
    }

    fun getEmail(): String = storage.getString("email", "") ?: ""
    fun getUid(): String = storage.getString("uid", "") ?: ""
    fun getRol(): String = storage.getString("rol", "") ?: ""
    fun getNombre(): String = storage.getString("nombre", "") ?: ""

    fun cerrarSesion() {
        storage.edit().clear().apply()
    }

    fun haySesion(): Boolean = getEmail().isNotEmpty()
}