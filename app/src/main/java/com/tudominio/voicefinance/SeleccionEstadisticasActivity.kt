package com.tudominio.voicefinance

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView

class SeleccionEstadisticasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_estadisticas)

        // CONFIGURACIÓN DEL BOTÓN DE REGRESAR
        val toolbar = findViewById<Toolbar>(R.id.toolbarSeleccionEstadisticas)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // BOTÓN 1: ESTADÍSTICAS GENERALES (Torta Global)
        findViewById<MaterialCardView>(R.id.cardEstadisticasGenerales).setOnClickListener {
            val intent = Intent(this, EstadisticasActivity::class.java)
            startActivity(intent)
        }

        // BOTÓN 2: ESTADÍSTICAS POR CARTERA (Listado de carteras)
        findViewById<MaterialCardView>(R.id.cardEstadisticasPorCartera).setOnClickListener {
            val intent = Intent(this, EstadisticasCarterasActivity::class.java)
            startActivity(intent)
        }

        // BOTÓN 3: ESTADÍSTICAS POR CATEGORÍA (¡CONECTADO AHORA!)
        findViewById<MaterialCardView>(R.id.cardEstadisticasPorCategoria).setOnClickListener {
            val intent = Intent(this, EstadisticasCategoriasActivity::class.java)
            startActivity(intent)
        }
    }
}