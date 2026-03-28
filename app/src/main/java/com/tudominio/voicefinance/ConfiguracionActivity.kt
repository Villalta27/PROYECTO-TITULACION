package com.tudominio.voicefinance

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView

class ConfiguracionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)

        // Toolbar con botón de regreso
        val toolbar = findViewById<Toolbar>(R.id.toolbarConfiguracion)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Clic en la opción de Categorías
        findViewById<MaterialCardView>(R.id.cardOpcionCategorias).setOnClickListener {
            val intent = Intent(this, GestionCategoriasActivity::class.java)
            startActivity(intent)
        }
    }
}