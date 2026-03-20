package com.tudominio.voicefinance

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

        findViewById<MaterialCardView>(R.id.cardEstadisticasGenerales).setOnClickListener {
            // Próximo paso: Abrir Estadísticas Generales
        }

        findViewById<MaterialCardView>(R.id.cardEstadisticasPorCartera).setOnClickListener {
            // Próximo paso: Abrir Estadísticas Por Cartera
        }
    }
}