package com.example.controlacademico.model

data class Asistencia(
    var id: String = "",
    val alumnoId: String = "",
    val alumnoNombre: String = "",
    val materiaId: String = "",
    val fecha: String = "",
    val presente: Boolean = true
)