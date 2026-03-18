package com.example.controlacademico.admin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controlacademico.MainActivity
import com.example.controlacademico.R
import com.example.controlacademico.databinding.ActivityAdminBinding
import com.example.controlacademico.model.Materia
import com.example.controlacademico.model.Usuario
import com.example.controlacademico.prefs.Prefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage


class AdminActivity : AppCompatActivity() {

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { subirFotoFirebase(it) }
    }

    private lateinit var binding: ActivityAdminBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        prefs = Prefs(this)

        binding.tvBienvenida.text = "Admin: ${prefs.getNombre()}"

        cargarFotoPerfil()

        binding.imgFotoPerfil.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnGestionarUsuarios.setOnClickListener {
            mostrarDialogoUsuarios()
        }

        binding.btnCrearMateria.setOnClickListener {
            mostrarDialogoCrearMateria()
        }

        binding.btnVerMaterias.setOnClickListener {
            mostrarMaterias()
        }

        binding.btnAsignarMateria.setOnClickListener {
            mostrarDialogoAsignar()
        }

        binding.btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            prefs.cerrarSesion()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun mostrarDialogoUsuarios() {
        db.collection("usuarios").get()
            .addOnSuccessListener { docs ->
                val usuarios = docs.map { doc ->
                    val u = doc.toObject(Usuario::class.java)
                    u.id = doc.id
                    u
                }
                val nombres = usuarios.map { "${it.nombre} (${it.rol})" }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Selecciona un usuario")
                    .setItems(nombres) { _, index ->
                        mostrarDialogoCambiarRol(usuarios[index])
                    }
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoCambiarRol(usuario: Usuario) {
        val roles = arrayOf("alumno", "profesor", "admin")
        val indexActual = roles.indexOf(usuario.rol)

        AlertDialog.Builder(this)
            .setTitle("Cambiar rol de ${usuario.nombre}")
            .setSingleChoiceItems(roles, indexActual) { dialog, index ->
                val nuevoRol = roles[index]
                db.collection("usuarios").document(usuario.id)
                    .update("rol", nuevoRol)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Rol actualizado a $nuevoRol", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar rol", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoCrearMateria() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_materia, null)
        val etNombre = view.findViewById<EditText>(R.id.etNombreMateria)
        val etHorario = view.findViewById<EditText>(R.id.etHorario)

        AlertDialog.Builder(this)
            .setTitle("Nueva Materia")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val horario = etHorario.text.toString().trim()

                if (nombre.isEmpty()) {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val materia = hashMapOf(
                    "nombre" to nombre,
                    "horario" to horario,
                    "profesorId" to "",
                    "profesorNombre" to "",
                    "alumnos" to listOf<String>()
                )

                db.collection("materias").add(materia)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Materia creada correctamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al crear materia", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarMaterias() {
        db.collection("materias").get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(this, "No hay materias creadas", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val lista = docs.map { doc ->
                    val nombre = doc.getString("nombre") ?: "Sin nombre"
                    val horario = doc.getString("horario") ?: "Sin horario"
                    "$nombre — $horario"
                }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Materias")
                    .setItems(lista, null)
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
    }

    private fun mostrarDialogoAsignar() {
        db.collection("materias").get()
            .addOnSuccessListener { materias ->
                if (materias.isEmpty) {
                    Toast.makeText(this, "No hay materias creadas", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val listaMaterias = materias.documents
                val nombres = listaMaterias.map {
                    it.getString("nombre") ?: "Sin nombre"
                }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Selecciona una materia")
                    .setItems(nombres) { _, index ->
                        val materiaId = listaMaterias[index].id
                        val materiaNombre = nombres[index]
                        mostrarOpcionesAsignacion(materiaId, materiaNombre)
                    }
                    .show()
            }
    }

    private fun mostrarOpcionesAsignacion(materiaId: String, materiaNombre: String) {
        val opciones = arrayOf("Asignar Profesor", "Agregar Alumno")

        AlertDialog.Builder(this)
            .setTitle(materiaNombre)
            .setItems(opciones) { _, index ->
                when (index) {
                    0 -> asignarProfesor(materiaId, materiaNombre)
                    1 -> agregarAlumno(materiaId, materiaNombre)
                }
            }
            .show()
    }

    private fun asignarProfesor(materiaId: String, materiaNombre: String) {
        db.collection("usuarios")
            .whereEqualTo("rol", "profesor")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(this, "No hay profesores registrados", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val profesores = docs.documents
                val nombres = profesores.map {
                    it.getString("nombre") ?: "Sin nombre"
                }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Asignar profesor a $materiaNombre")
                    .setItems(nombres) { _, index ->
                        val profesorId = profesores[index].id
                        val profesorNombre = nombres[index]

                        db.collection("materias").document(materiaId)
                            .update(
                                mapOf(
                                    "profesorId" to profesorId,
                                    "profesorNombre" to profesorNombre
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "$profesorNombre asignado a $materiaNombre",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al asignar profesor", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .show()
            }
    }

    private fun agregarAlumno(materiaId: String, materiaNombre: String) {
        db.collection("usuarios")
            .whereEqualTo("rol", "alumno")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(this, "No hay alumnos registrados", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val alumnos = docs.documents
                val nombres = alumnos.map {
                    it.getString("nombre") ?: "Sin nombre"
                }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Agregar alumno a $materiaNombre")
                    .setItems(nombres) { _, index ->
                        val alumnoId = alumnos[index].id
                        val alumnoNombre = nombres[index]

                        // Usamos FieldValue.arrayUnion para agregar sin borrar los demás
                        db.collection("materias").document(materiaId)
                            .update(
                                "alumnos",
                                com.google.firebase.firestore.FieldValue.arrayUnion(alumnoId)
                            )
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "$alumnoNombre agregado a $materiaNombre",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al agregar alumno", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .show()
            }
    }

    private fun subirFotoFirebase(uri: Uri) {
        val uid = prefs.getUid()
        val storageRef = FirebaseStorage.getInstance()
            .reference.child("fotos/$uid.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    FirebaseFirestore.getInstance()
                        .collection("usuarios").document(uid)
                        .update("fotoPerfil", downloadUri.toString())
                        .addOnSuccessListener {
                            Glide.with(this)
                                .load(downloadUri)
                                .circleCrop()
                                .into(binding.imgFotoPerfil)
                            Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir foto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarFotoPerfil() {
        val uid = prefs.getUid()
        FirebaseFirestore.getInstance()
            .collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val foto = doc.getString("fotoPerfil")
                if (!foto.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(foto)
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .into(binding.imgFotoPerfil)
                }
            }
    }


}