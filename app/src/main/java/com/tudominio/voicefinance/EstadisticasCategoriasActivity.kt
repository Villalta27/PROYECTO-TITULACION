package com.tudominio.voicefinance

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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

class EstadisticasCategoriasActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvTop: RecyclerView
    private lateinit var rvCarteras: RecyclerView
    private lateinit var toolbar: Toolbar

    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null
    private var registroEscucha: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas_categorias)

        rvTop = findViewById(R.id.rvTopCategorias)
        rvTop.layoutManager = LinearLayoutManager(this)

        rvCarteras = findViewById(R.id.rvCarterasParaCategorias)
        rvCarteras.layoutManager = LinearLayoutManager(this)

        setupToolbar()

        findViewById<ImageButton>(R.id.btnFiltroCategoriasGeneral).setOnClickListener {
            mostrarSelectorFechas()
        }

        findViewById<ImageButton>(R.id.btnLimpiarFiltroCategorias).setOnClickListener {
            limpiarFiltros()
        }

        cargarDatosRanking()
        cargarListaCarteras()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbarCategorias)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun mostrarSelectorFechas() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val calI = Calendar.getInstance(); calI.set(y, m, d, 0, 0, 0); fechaInicio = calI.time
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val calF = Calendar.getInstance(); calF.set(y2, m2, d2, 23, 59, 59); fechaFin = calF.time
                cargarDatosRanking()
                Toast.makeText(this, "Ranking filtrado", Toast.LENGTH_SHORT).show()
            }, y, m, d).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun limpiarFiltros() {
        if (fechaInicio == null && fechaFin == null) {
            Toast.makeText(this, "Mostrando historial total", Toast.LENGTH_SHORT).show()
            return
        }
        fechaInicio = null
        fechaFin = null
        cargarDatosRanking()
        Toast.makeText(this, "Filtros eliminados", Toast.LENGTH_SHORT).show()
    }

    private fun cargarDatosRanking() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        registroEscucha?.remove()

        var consulta = db.collection("transacciones")
            .whereEqualTo("userId", uId)
            .whereEqualTo("tipo", "gasto")

        if (fechaInicio != null && fechaFin != null) {
            consulta = consulta.whereGreaterThanOrEqualTo("fecha", Timestamp(fechaInicio!!))
                .whereLessThanOrEqualTo("fecha", Timestamp(fechaFin!!))
            supportActionBar?.subtitle = "Ranking por Periodo"
            toolbar.setSubtitleTextColor(Color.parseColor("#4CC9F0"))
        } else {
            supportActionBar?.subtitle = "Ranking Histórico"
            toolbar.setSubtitleTextColor(Color.parseColor("#E0E0E0"))
        }

        registroEscucha = consulta.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener
            val mapaCategorias = mutableMapOf<String, Double>()
            for (doc in snapshots) {
                val cat = doc.getString("categoria") ?: "Otros"
                val monto = doc.getDouble("monto") ?: 0.0
                if (cat.contains("Ahorro", ignoreCase = true)) continue
                mapaCategorias[cat] = (mapaCategorias[cat] ?: 0.0) + abs(monto)
            }
            val rankingOrdenado = mapaCategorias.toList().sortedByDescending { it.second }
            mostrarPodio(rankingOrdenado)
            val colores = listOf("#FFD700", "#C0C0C0", "#CD7F32", "#4CC9F0", "#4361EE", "#7209B7", "#F72585")
            val listaParaAdapter = rankingOrdenado.mapIndexed { index, pair ->
                RankingCategoriasAdapter.CategoriaRanking(pair.first, pair.second, colores[index % colores.size])
            }
            rvTop.adapter = RankingCategoriasAdapter(listaParaAdapter)
        }
    }

    private fun mostrarPodio(lista: List<Pair<String, Double>>) {
        val views = listOf(R.id.tvNombreOro to R.id.tvMontoOro, R.id.tvNombrePlata to R.id.tvMontoPlata, R.id.tvNombreBronce to R.id.tvMontoBronce)
        views.forEach { (nomId, monId) ->
            findViewById<TextView>(nomId).text = "---"
            findViewById<TextView>(monId).text = "$0"
        }
        if (lista.isNotEmpty()) {
            findViewById<TextView>(R.id.tvNombreOro).text = lista[0].first
            findViewById<TextView>(R.id.tvMontoOro).text = "$${lista[0].second.toInt()}"
        }
        if (lista.size > 1) {
            findViewById<TextView>(R.id.tvNombrePlata).text = lista[1].first
            findViewById<TextView>(R.id.tvMontoPlata).text = "$${lista[1].second.toInt()}"
        }
        if (lista.size > 2) {
            findViewById<TextView>(R.id.tvNombreBronce).text = lista[2].first
            findViewById<TextView>(R.id.tvMontoBronce).text = "$${lista[2].second.toInt()}"
        }
    }

    private fun cargarListaCarteras() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        db.collection("carteras").whereEqualTo("userId", uId).get().addOnSuccessListener { snapshots ->
            val lista = snapshots.documents.map { mapOf("id" to it.id, "nombre" to (it.getString("nombre") ?: "")) }
            rvCarteras.adapter = CarteraEstadisticaAdapter(lista) { item ->
                val intent = Intent(this, DetalleCategoriasPorCarteraActivity::class.java)
                intent.putExtra("CARTERA_ID", item["id"])
                intent.putExtra("CARTERA_NOMBRE", item["nombre"])
                startActivity(intent)
            }
        }
    }
} // <--- ESTA ES LA ÚLTIMA LLAVE QUE DEBE HABER