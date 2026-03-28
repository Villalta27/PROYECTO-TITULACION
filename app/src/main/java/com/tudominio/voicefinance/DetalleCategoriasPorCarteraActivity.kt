package com.tudominio.voicefinance

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.math.abs

class DetalleCategoriasPorCarteraActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvTop: RecyclerView
    private lateinit var toolbar: Toolbar
    private var carteraId: String? = null

    // VARIABLES PARA FILTROS
    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null
    private var registroEscucha: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas_categorias)

        carteraId = intent.getStringExtra("CARTERA_ID") ?: return
        val nombreCartera = intent.getStringExtra("CARTERA_NOMBRE") ?: "Cartera"

        rvTop = findViewById(R.id.rvTopCategorias)
        rvTop.layoutManager = LinearLayoutManager(this)

        // Limpiar interfaz compartida
        findViewById<RecyclerView>(R.id.rvCarterasParaCategorias).visibility = View.GONE
        findViewById<TextView>(R.id.tvTituloMirarPorCartera)?.visibility = View.GONE

        setupToolbar(nombreCartera)

        // Configurar Botones de Filtro
        findViewById<ImageButton>(R.id.btnFiltroCategoriasGeneral).setOnClickListener {
            mostrarSelectorFechas()
        }

        findViewById<ImageButton>(R.id.btnLimpiarFiltroCategorias).setOnClickListener {
            limpiarFiltros()
        }

        cargarDatosRankingFiltrado()
    }

    private fun setupToolbar(nombre: String) {
        toolbar = findViewById(R.id.toolbarCategorias)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Gastos: $nombre"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun mostrarSelectorFechas() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val calI = Calendar.getInstance(); calI.set(y, m, d, 0, 0, 0); fechaInicio = calI.time
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val calF = Calendar.getInstance(); calF.set(y2, m2, d2, 23, 59, 59); fechaFin = calF.time
                cargarDatosRankingFiltrado()
            }, y, m, d).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun limpiarFiltros() {
        if (fechaInicio == null && fechaFin == null) return
        fechaInicio = null
        fechaFin = null
        cargarDatosRankingFiltrado()
        Toast.makeText(this, "Filtros eliminados", Toast.LENGTH_SHORT).show()
    }

    private fun cargarDatosRankingFiltrado() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        val cId = carteraId ?: return

        registroEscucha?.remove()

        var consulta = db.collection("transacciones")
            .whereEqualTo("userId", uId)
            .whereEqualTo("carteraId", cId)
            .whereEqualTo("tipo", "gasto")

        if (fechaInicio != null && fechaFin != null) {
            consulta = consulta.whereGreaterThanOrEqualTo("fecha", Timestamp(fechaInicio!!))
                .whereLessThanOrEqualTo("fecha", Timestamp(fechaFin!!))
            supportActionBar?.subtitle = "Periodo Seleccionado"
            toolbar.setSubtitleTextColor(Color.parseColor("#4CC9F0"))
        } else {
            supportActionBar?.subtitle = "Historial Completo"
            toolbar.setSubtitleTextColor(Color.parseColor("#E0E0E0"))
        }

        registroEscucha = consulta.addSnapshotListener { snapshots, _ ->
            if (snapshots == null) return@addSnapshotListener
            val mapaCategorias = mutableMapOf<String, Double>()
            for (doc in snapshots) {
                val cat = doc.getString("categoria") ?: "Otros"
                if (cat.contains("Ahorro", ignoreCase = true)) continue
                val monto = doc.getDouble("monto") ?: 0.0
                mapaCategorias[cat] = (mapaCategorias[cat] ?: 0.0) + abs(monto)
            }
            val ranking = mapaCategorias.toList().sortedByDescending { it.second }
            mostrarPodio(ranking)

            val colores = listOf("#FFD700", "#C0C0C0", "#CD7F32", "#4CC9F0", "#4361EE", "#7209B7", "#F72585")
            val lista = ranking.mapIndexed { i, p ->
                RankingCategoriasAdapter.CategoriaRanking(p.first, p.second, colores[i % colores.size])
            }
            rvTop.adapter = RankingCategoriasAdapter(lista)
        }
    }

    private fun mostrarPodio(lista: List<Pair<String, Double>>) {
        val tvs = listOf(R.id.tvNombreOro to R.id.tvMontoOro, R.id.tvNombrePlata to R.id.tvMontoPlata, R.id.tvNombreBronce to R.id.tvMontoBronce)
        tvs.forEach { (n, m) ->
            findViewById<TextView>(n).text = "---"
            findViewById<TextView>(m).text = "$0"
        }
        lista.take(3).forEachIndexed { i, p ->
            findViewById<TextView>(tvs[i].first).text = p.first
            findViewById<TextView>(tvs[i].second).text = "$${p.second.toInt()}"
        }
    }
} // <--- ASEGÚRATE DE QUE ESTA SEA LA ÚLTIMA LLAVE