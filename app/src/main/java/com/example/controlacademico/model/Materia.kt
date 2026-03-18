package com.example.controlacademico.model

data class Materia(
    var id: String = "",
    val nombre: String = "",
    val profesorId: String = "",
    val profesorNombre: String = "",
    val alumnos: List<String> = emptyList(),  // lista de UIDs de alumnos
    val horario: String = ""
)