package com.example.controlacademico.alumno

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.controlacademico.MainActivity
import com.example.controlacademico.databinding.ActivityAlumnoBinding
import com.example.controlacademico.prefs.Prefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.example.controlacademico.R
class AlumnoActivity : AppCompatActivity() {

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { subirFotoFirebase(it) }
    }

    private lateinit var binding: ActivityAlumnoBinding
    private lateinit var prefs: Prefs
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlumnoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        prefs = Prefs(this)

        binding.tvBienvenida.text = "Bienvenido, ${prefs.getNombre()}"

        // Generar QR automáticamente con el UID del alumno
        generarQR(prefs.getUid())

        binding.btnVerCalificaciones.setOnClickListener {
            verCalificaciones()
        }

        binding.btnVerHorario.setOnClickListener {
            verHorario()
        }

        binding.btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            prefs.cerrarSesion()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        cargarFotoPerfil()

        binding.btnCambiarFoto.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun generarQR(uid: String) {
        try {
            val writer = MultiFormatWriter()
            val matrix = writer.encode(uid, BarcodeFormat.QR_CODE, 512, 512)
            val encoder = BarcodeEncoder()
            val bitmap: Bitmap = encoder.createBitmap(matrix)
            binding.imgQR.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al generar QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verCalificaciones() {
        val uid = prefs.getUid()
        db.collection("calificaciones")
            .whereEqualTo("alumnoId", uid)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(this, "No tienes calificaciones aún", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val lista = docs.map { doc ->
                    val materia = doc.getString("materiaNombre") ?: "Sin materia"
                    val cal = doc.getLong("calificacion") ?: 0
                    "$materia: $cal"
                }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Mis Calificaciones")
                    .setItems(lista, null)
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar calificaciones", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verHorario() {
        val uid = prefs.getUid()
        db.collection("materias")
            .whereArrayContains("alumnos", uid)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(this, "No tienes materias asignadas aún", Toast.LENGTH_SHORT).show()
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
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar horario", Toast.LENGTH_SHORT).show()
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