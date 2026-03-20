package com.tudominio.voicefinance

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class HistorialDetalleActivity : AppCompatActivity() {

    private lateinit var tvTotal: TextView
    private lateinit var rv: RecyclerView
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_detalle)

        val carteraId = intent.getStringExtra("carteraId") ?: ""
        val nombre = intent.getStringExtra("carteraNombre") ?: "Historial"

        val toolbar = findViewById<Toolbar>(R.id.toolbarDetalle)
        setSupportActionBar(toolbar)
        supportActionBar?.title = nombre
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        tvTotal = findViewById(R.id.tvTotalGastosEspecíficos)
        rv = findViewById(R.id.rvHistorialGastosLista)
        rv.layoutManager = LinearLayoutManager(this)

        cargarTransaccionesDeEstaCartera(carteraId)
    }

    private fun cargarTransaccionesDeEstaCartera(cId: String) {
        db.collection("transacciones")
            .whereEqualTo("carteraId", cId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val listaMaps = mutableListOf<Map<String, Any>>()
                    var sumaGastosOnly = 0.0

                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        val mutableData = data.toMutableMap()
                        mutableData["id"] = doc.id
                        listaMaps.add(mutableData)

                        val tipo = data["tipo"]?.toString() ?: "gasto"
                        val monto = data["monto"]?.toString()?.toDoubleOrNull() ?: 0.0

                        if (tipo == "gasto") {
                            sumaGastosOnly += monto
                        }
                    }

                    tvTotal.text = String.format(Locale.US, "$ %.2f", sumaGastosOnly)

                    // MODIFICACIÓN AQUÍ: Pasamos la lista y una función vacía para el clic de edición
                    rv.adapter = GastoAdapter(listaMaps) { _, _ ->
                        // En el historial no permitimos edición por ahora para evitar conflictos
                    }
                }
            }
    }
}