package com.tudominio.voicefinance

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Sincronizado con TU XML original
        val etNombre = findViewById<EditText>(R.id.etNombreRegister)
        val etEmail = findViewById<EditText>(R.id.etEmailRegister)
        val etPassword = findViewById<EditText>(R.id.etPasswordRegister)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        btnRegister.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (nombre.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                if (pass.length >= 6) {
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                if (userId != null) {
                                    val datosUsuario = hashMapOf(
                                        "nombre" to nombre,
                                        "email" to email,
                                        "uid" to userId
                                    )
                                    // Guardamos en la colección usuarios
                                    db.collection("usuarios").document(userId)
                                        .set(datosUsuario)
                                        .addOnSuccessListener {

                                            // --- INICIO LÓGICA CORREO AUTOMÁTICO ---
                                            val ticketCorreo = hashMapOf(
                                                "to" to email,
                                                "message" to hashMapOf(
                                                    "subject" to "¡Bienvenido a VoiceFinance!",
                                                    "text" to "Hola $nombre, tu cuenta ha sido creada exitosamente en VoiceFinance."
                                                )
                                            )
                                            db.collection("mail").add(ticketCorreo)
                                            // --- FIN LÓGICA CORREO ---

                                            Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, FinanzasActivity::class.java))
                                            finish()
                                        }
                                }
                            } else {
                                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        tvLoginLink.setOnClickListener {
            finish()
        }
    }
}