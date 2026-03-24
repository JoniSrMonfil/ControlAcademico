package com.example.controlacademico.alumno

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.controlacademico.BaseActivity
import com.example.controlacademico.MainActivity
import com.example.controlacademico.databinding.ActivityAlumnoBinding
import com.example.controlacademico.prefs.Prefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.example.controlacademico.SettingsActivity
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
class AlumnoActivity : BaseActivity() {

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
        binding.btnAjustes.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
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

        // Convertir imagen a Base64
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes == null) {
            Toast.makeText(this, "Error al leer la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        // Comprimir — máximo 200KB para Firestore
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 40, outputStream)
        val base64 = android.util.Base64.encodeToString(
            outputStream.toByteArray(),
            android.util.Base64.DEFAULT
        )

        // Guardar en Firestore
        FirebaseFirestore.getInstance()
            .collection("usuarios").document(uid)
            .update("fotoPerfil", base64)
            .addOnSuccessListener {
                // Mostrar en la UI
                val decoded = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                binding.imgFotoPerfil.setImageBitmap(bmp)
                Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar foto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarFotoPerfil() {
        val uid = prefs.getUid()
        FirebaseFirestore.getInstance()
            .collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val base64 = doc.getString("fotoPerfil")
                if (!base64.isNullOrEmpty()) {
                    val decoded = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    binding.imgFotoPerfil.setImageBitmap(bmp)
                }
            }
    }

}