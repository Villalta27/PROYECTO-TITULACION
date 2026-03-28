package com.tudominio.voicefinance

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AbonosMetaActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvAbonos: RecyclerView
    private var metaId: String = ""
    private var metaNombre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_abonos_meta)

        metaId = intent.getStringExtra("META_ID") ?: ""
        metaNombre = intent.getStringExtra("META_NOMBRE") ?: "Detalle"

        val toolbar = findViewById<Toolbar>(R.id.toolbarAbonos)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Abonos: $metaNombre"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rvAbonos = findViewById(R.id.rvAbonos)
        rvAbonos.layoutManager = LinearLayoutManager(this)

        // BOTÓN DE REVERSO
        findViewById<MaterialButton>(R.id.btnReversarAhorro).setOnClickListener {
            mostrarDialogoReverso()
        }

        cargarHistorialDeAbonos(metaId)
    }

    private fun mostrarDialogoReverso() {
        val uId = Firebase.auth.currentUser?.uid ?: return

        // 1. Buscamos las carteras reales del usuario
        db.collection("carteras").whereEqualTo("userId", uId).get()
            .addOnSuccessListener { snapshots ->
                if (snapshots.isEmpty) {
                    Toast.makeText(this, "No tienes carteras para devolver el dinero", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val nombresCarteras = snapshots.map { it.getString("nombre") ?: "Sin nombre" }.toTypedArray()
                val idsCarteras = snapshots.map { it.id }.toTypedArray()

                val builder = AlertDialog.Builder(this)
                builder.setTitle("REVERSAR AHORRO")

                val layout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 10)
                }

                val txtInfo = TextView(this).apply { text = "Selecciona cartera de destino:" }
                val spinner = Spinner(this)
                spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresCarteras)

                val etMonto = EditText(this).apply {
                    hint = "Monto a devolver $"
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                }

                layout.addView(txtInfo)
                layout.addView(spinner)
                layout.addView(etMonto)
                builder.setView(layout)

                builder.setPositiveButton("REVERSAR") { _, _ ->
                    val monto = etMonto.text.toString().toDoubleOrNull() ?: 0.0
                    val cId = idsCarteras[spinner.selectedItemPosition]
                    val cNombre = nombresCarteras[spinner.selectedItemPosition]

                    if (monto > 0) {
                        ejecutarOperacionReverso(cId, cNombre, monto)
                    } else {
                        Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("CANCELAR", null)
                builder.show()
            }
    }

    private fun ejecutarOperacionReverso(carteraId: String, nombreCartera: String, monto: Double) {
        val batch = db.batch()
        val fecha = Timestamp.now()
        val uId = Firebase.auth.currentUser?.uid ?: return

        // 1. Restar de la Meta de Ahorro (ahorradoActual)
        val refMeta = db.collection("metas_ahorro").document(metaId)
        batch.update(refMeta, "ahorradoActual", FieldValue.increment(-monto))

        // 2. Sumar a la Cartera (se registra como transaccion de tipo ingreso)
        val refTransaccion = db.collection("transacciones").document()
        val dataTransaccion = hashMapOf(
            "titulo" to "RETORNO: $metaNombre",
            "monto" to monto,
            "tipo" to "ingreso", // IMPORTANTE: Es ingreso para la cartera
            "categoria" to "Ingreso",
            "fecha" to fecha,
            "userId" to uId,
            "carteraId" to carteraId
        )
        batch.set(refTransaccion, dataTransaccion)

        // 3. Registrar en el historial de la meta (Monto negativo para indicar salida)
        val refAbono = db.collection("abonos_metas").document()
        val dataAbono = hashMapOf(
            "metaId" to metaId,
            "monto" to -monto, // Negativo para que el historial refleje la resta
            "fecha" to fecha,
            "desdeCartera" to "Reverso a: $nombreCartera"
        )
        batch.set(refAbono, dataAbono)

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Dinero devuelto a $nombreCartera", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al reversar: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarHistorialDeAbonos(mId: String) {
        db.collection("abonos_metas")
            .whereEqualTo("metaId", mId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
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