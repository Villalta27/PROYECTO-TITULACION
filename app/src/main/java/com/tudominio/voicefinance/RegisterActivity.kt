package com.tudominio.voicefinance

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    // Declaramos la variable para Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 1. Inicializar Firebase Auth
        auth = Firebase.auth

        // 2. Vincular los componentes del XML por su ID
        val etEmail = findViewById<EditText>(R.id.etEmailRegister)
        val etPassword = findViewById<EditText>(R.id.etPasswordRegister)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        // 3. Configurar el evento del botón de Registro
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validación de campos vacíos (XP: Simplicidad y Prevención de errores)
            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (password.length >= 6) {
                    crearCuenta(email, password)
                } else {
                    Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Volver al Login si el usuario ya tiene cuenta
        tvLoginLink.setOnClickListener {
            finish() // Cierra esta pantalla y regresa a la anterior (MainActivity)
        }
    }

    private fun crearCuenta(email: String, password: String) {
        // 4. Llamada al método de Firebase para crear el usuario
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Éxito: El usuario se guardó en la base de datos de Firebase
                    Toast.makeText(this, "¡Usuario registrado con éxito!", Toast.LENGTH_LONG).show()
                    finish() // Regresa al Login automáticamente
                } else {
                    // Error: Por ejemplo, el correo ya está en uso o no tiene formato válido
                    Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}