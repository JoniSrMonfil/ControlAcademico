package com.example.controlacademico.model

data class Usuario(
    var id: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: String = "alumno"   // valores posibles: "admin", "profesor", "alumno"
)