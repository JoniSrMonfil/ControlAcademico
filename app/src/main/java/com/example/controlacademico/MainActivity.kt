package com.example.controlacademico

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.controlacademico.admin.AdminActivity
import com.example.controlacademico.alumno.AlumnoActivity
import com.example.controlacademico.databinding.ActivityMainBinding
import com.example.controlacademico.prefs.Prefs
import com.example.controlacademico.profesor.ProfesorActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        prefs = Prefs(this)

        if (prefs.haySesion()) {
            irARolCorrespondiente(prefs.getRol())
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loginFirebase(email, pass)
        }

        binding.btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginFirebase(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val rol = doc.getString("rol") ?: "alumno"
                        val nombre = doc.getString("nombre") ?: email
                        prefs.guardarSesion(email, uid, rol, nombre)
                        irARolCorrespondiente(rol)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al obtener el rol", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun irARolCorrespondiente(rol: String) {
        val destino = when (rol) {
            "admin"    -> AdminActivity::class.java
            "profesor" -> ProfesorActivity::class.java
            else       -> AlumnoActivity::class.java
        }
        startActivity(Intent(this, destino))
        finish()
    }
}