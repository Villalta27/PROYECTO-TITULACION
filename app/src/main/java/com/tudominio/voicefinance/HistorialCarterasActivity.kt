package com.tudominio.voicefinance

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HistorialCarterasActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_gastos)

        val toolbar = findViewById<Toolbar>(R.id.toolbarCarteras)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rv = findViewById(R.id.rvHistorialGastos)
        rv.layoutManager = LinearLayoutManager(this)

        cargarCarteras()
    }

    private fun cargarCarteras() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("carteras")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    // Convertimos el snapshot a una lista de Mapas como pide tu adaptador
                    val listaMaps = snapshot.documents.map { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data
                    }

                    // Le pasamos los 3 parámetros: lista, onItemClick y onOpcionesClick
                    val adapter = FinanzaAdapter(
                        listaMaps,
                        { id, nombre -> // Acción al tocar la cartera
                            val intent = Intent(this, HistorialDetalleActivity::class.java)
                            intent.putExtra("carteraId", id)
                            intent.putExtra("carteraNombre", nombre)
                            startActivity(intent)
                        },
                        { id, nombre, vista ->
                            // Aquí no necesitamos opciones en el historial, lo dejamos vacío
                        }
                    )
                    rv.adapter = adapter
                }
            }
    }
}