package com.example.controlacademico.profesor

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.controlacademico.MainActivity
import com.example.controlacademico.databinding.ActivityProfesorBinding
import com.example.controlacademico.prefs.Prefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.example.controlacademico.R

class ProfesorActivity : AppCompatActivity() {

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { subirFotoFirebase(it) }
    }

    private lateinit var binding: ActivityProfesorBinding
    private lateinit var prefs: Prefs
    private lateinit var db: FirebaseFirestore

    // Lanzador del escáner QR
    private val escanerQR = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            registrarAsistencia(result.contents)
        } else {
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfesorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        prefs = Prefs(this)

        binding.tvBienvenida.text = "Profesor: ${prefs.getNombre()}"

        cargarFotoPerfil()

        binding.imgFotoPerfil.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnEscanearQR.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Escanea el QR del alumno")
            options.setBeepEnabled(true)
            options.setOrientationLocked(true)
            escanerQR.launch(options)
        }

        binding.btnCalificaciones.setOnClickListener {
            mostrarAlumnosParaCalificar()
        }

        binding.btnHorario.setOnClickListener {
            mostrarHorario()
        }

        binding.btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            prefs.cerrarSesion()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }


    }

    private fun registrarAsistencia(contenidoQR: String) {
        // El QR contiene el UID del alumno
        val alumnoId = contenidoQR

        db.collection("usuarios").document(alumnoId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nombreAlumno = doc.getString("nombre") ?: "Desconocido"

                    // Seleccionar la materia para la asistencia
                    db.collection("materias").get()
                        .addOnSuccessListener { materias ->
                            val listaMaterias = materias.documents
                            val nombres = listaMaterias.map {
                                it.getString("nombre") ?: "Sin nombre"
                            }.toTypedArray()

                            AlertDialog.Builder(this)
                                .setTitle("¿En qué materia registrar a $nombreAlumno?")
                                .setItems(nombres) { _, index ->
                                    val materiaId = listaMaterias[index].id
                                    val materiaNombre = nombres[index]
                                    guardarAsistencia(alumnoId, nombreAlumno, materiaId, materiaNombre)
                                }
                                .show()
                        }
                } else {
                    Toast.makeText(this, "Alumno no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun guardarAsistencia(
        alumnoId: String,
        nombreAlumno: String,
        materiaId: String,
        materiaNombre: String
    ) {
        val fecha = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm", java.util.Locale.getDefault()
        ).format(java.util.Date())

        val asistencia = hashMapOf(
            "alumnoId" to alumnoId,
            "alumnoNombre" to nombreAlumno,
            "materiaId" to materiaId,
            "materiaNombre" to materiaNombre,
            "fecha" to fecha,
            "presente" to true
        )

        db.collection("asistencias").add(asistencia)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Asistencia de $nombreAlumno registrada en $materiaNombre",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar asistencia", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarAlumnosParaCalificar() {
        db.collection("usuarios")
            .whereEqualTo("rol", "alumno")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(this, "No hay alumnos registrados", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val alumnos = docs.documents
                val nombres = alumnos.map { it.getString("nombre") ?: "Sin nombre" }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Selecciona un alumno")
                    .setItems(nombres) { _, index ->
                        val alumnoId = alumnos[index].id
                        val nombreAlumno = nombres[index]
                        mostrarDialogoCalificacion(alumnoId, nombreAlumno)
                    }
                    .show()
            }
    }

    private fun mostrarDialogoCalificacion(alumnoId: String, nombreAlumno: String) {
        db.collection("materias").get()
            .addOnSuccessListener { materias ->
                val listaMaterias = materias.documents
                val nombres = listaMaterias.map {
                    it.getString("nombre") ?: "Sin nombre"
                }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("¿En qué materia calificar a $nombreAlumno?")
                    .setItems(nombres) { _, index ->
                        val materiaId = listaMaterias[index].id
                        val materiaNombre = nombres[index]

                        val etCalificacion = EditText(this)
                        etCalificacion.hint = "Calificación (0-100)"
                        etCalificacion.inputType = android.text.InputType.TYPE_CLASS_NUMBER

                        AlertDialog.Builder(this)
                            .setTitle("Calificación de $nombreAlumno en $materiaNombre")
                            .setView(etCalificacion)
                            .setPositiveButton("Guardar") { _, _ ->
                                val calificacion = etCalificacion.text.toString()
                                if (calificacion.isNotEmpty()) {
                                    guardarCalificacion(alumnoId, nombreAlumno, materiaId, materiaNombre, calificacion)
                                }
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                    .show()
            }
    }

    private fun guardarCalificacion(
        alumnoId: String,
        nombreAlumno: String,
        materiaId: String,
        materiaNombre: String,
        calificacion: String
    ) {
        val datos = hashMapOf(
            "alumnoId" to alumnoId,
            "alumnoNombre" to nombreAlumno,
            "materiaId" to materiaId,
            "materiaNombre" to materiaNombre,
            "calificacion" to calificacion.toInt()
        )

        db.collection("calificaciones").add(datos)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Calificación guardada: $calificacion",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar calificación", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarHorario() {
        val uid = prefs.getUid()
        db.collection("materias")
            .whereEqualTo("profesorId", uid)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(this, "No tienes materias asignadas", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val lista = docs.map { doc ->
                    val nombre = doc.getString("nombre") ?: "Sin nombre"
                    val horario = doc.getString("horario") ?: "Sin horario"
                    "$nombre — $horario"
                }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Mi Horario")
                    .setItems(lista, null)
                    .setNegativeButton("Cerrar", null)
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
                    // Guardar URL en Firestore
                    FirebaseFirestore.getInstance()
                        .collection("usuarios").document(uid)
                        .update("fotoPerfil", downloadUri.toString())
                        .addOnSuccessListener {
                            // Mostrar en la UI
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