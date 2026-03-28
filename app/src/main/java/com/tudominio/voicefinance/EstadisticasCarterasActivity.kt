package com.tudominio.voicefinance

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.abs

class EstadisticasCarterasActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvCarteras: RecyclerView
    private var idCarteraTop: String? = null
    private var nombreCarteraTop: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas_carteras)

        rvCarteras = findViewById(R.id.rvListaCarterasEstadisticas)
        rvCarteras.layoutManager = LinearLayoutManager(this)

        // --- CONFIGURACIÓN DE LA TOOLBAR Y BOTÓN DE REGRESAR ---
        val toolbar = findViewById<Toolbar>(R.id.toolbarEstadisticasCarteras)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Estadísticas por Cartera"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        // -------------------------------------------------------

        val btnVerTop = findViewById<MaterialButton>(R.id.btnVerEstadisticasTop)
        btnVerTop.setOnClickListener {
            idCarteraTop?.let { id -> abrirDetalle(id, nombreCarteraTop ?: "Cartera") }
        }

        calcularCarteraMasUsada()
        cargarListaCarteras()
    }

    // --- MANEJO DEL BOTÓN DE REGRESAR (Mismo patrón que tus otras ventanas) ---
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    // -------------------------------------------------------------------------

    private fun calcularCarteraMasUsada() {
        val uId = Firebase.auth.currentUser?.uid ?: return

        db.collection("transacciones").whereEqualTo("userId", uId).get()
            .addOnSuccessListener { snapshots ->
                val movimientosPorCartera = mutableMapOf<String, Double>()

                for (doc in snapshots) {
                    val cId = doc.getString("carteraId") ?: continue
                    val monto = doc.getDouble("monto") ?: 0.0
                    // Sumamos el valor absoluto para medir la actividad total (dinero que fluye)
                    movimientosPorCartera[cId] = (movimientosPorCartera[cId] ?: 0.0) + abs(monto)
                }

                val topEntry = movimientosPorCartera.maxByOrNull { it.value }
                if (topEntry != null) {
                    idCarteraTop = topEntry.key
                    obtenerNombreYMostrar(topEntry.key, topEntry.value)
                }
            }
    }

    private fun obtenerNombreYMostrar(id: String, montoTotal: Double) {
        db.collection("carteras").document(id).get().addOnSuccessListener {
            nombreCarteraTop = it.getString("nombre") ?: "Cartera"
            findViewById<TextView>(R.id.tvNombreCarteraTop).text = nombreCarteraTop
            findViewById<TextView>(R.id.tvMontoMovidoTop).text = "$ ${String.format("%.2f", montoTotal)} movidos"
        }
    }

    private fun cargarListaCarteras() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        db.collection("carteras").whereEqualTo("userId", uId).get()
            .addOnSuccessListener { snapshots ->
                // Mapeamos los datos a un formato que el adaptador entienda (Map de Strings)
                val lista = snapshots.documents.map { doc ->
                    mapOf(
                        "id" to doc.id,
                        "nombre" to (doc.getString("nombre") ?: "Sin nombre")
                    )
                }

                // Usamos el adaptador que sí existe
                rvCarteras.adapter = CarteraEstadisticaAdapter(lista) { item ->
                    abrirDetalle(item["id"]!!, item["nombre"]!!)
                }
            }
    }

    private fun abrirDetalle(id: String, nombre: String) {
        val intent = Intent(this, DetalleEstadisticaCarteraActivity::class.java)
        intent.putExtra("CARTERA_ID", id)
        intent.putExtra("CARTERA_NOMBRE", nombre)
        startActivity(intent)
    }
}