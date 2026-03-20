package com.tudominio.voicefinance

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AbonosMetaActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvAbonos: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_abonos_meta)

        val metaId = intent.getStringExtra("META_ID") ?: ""
        val metaNombre = intent.getStringExtra("META_NOMBRE") ?: "Detalle"

        val toolbar = findViewById<Toolbar>(R.id.toolbarAbonos)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Abonos: $metaNombre"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rvAbonos = findViewById(R.id.rvAbonos)
        rvAbonos.layoutManager = LinearLayoutManager(this)

        cargarHistorialDeAbonos(metaId)
    }

    private fun cargarHistorialDeAbonos(mId: String) {
        db.collection("abonos_metas")
            .whereEqualTo("metaId", mId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // Si te sale error de índice aquí, recuerda el link de Firebase que vimos antes
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val lista = snapshots.documents.mapNotNull { it.data }
                    rvAbonos.adapter = AbonosAdapter(lista)
                }
            }
    }
}