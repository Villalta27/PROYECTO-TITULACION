package com.tudominio.voicefinance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnComenzar = findViewById<Button>(R.id.btnComenzar)

        btnComenzar.setOnClickListener {
            // Saltamos a la pantalla principal de la app
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Cerramos esta actividad para limpiar la pila de navegación
        }
    }
}