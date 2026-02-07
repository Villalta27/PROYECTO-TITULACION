package com.tudominio.voicefinance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Usando el ID correcto de tu proyecto: btnComenzar
        val btnComenzar = findViewById<Button>(R.id.btnComenzar)

        btnComenzar.setOnClickListener {
            // Guardamos la preferencia para que no vuelva a aparecer esta pantalla
            val sharedPref = getSharedPreferences("ConfiguracionApp", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("es_nuevo_usuario", false)
                apply()
            }

            // Saltamos a la nueva pantalla principal: Lista de Carteras
            val intent = Intent(this, FinanzasActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}