package com.tudominio.voicefinance

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class HomeActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvTransacciones: RecyclerView
    private lateinit var tvSaldoCartera: TextView
    private var carteraId: String? = null
    private var saldoActualCalculado: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        carteraId = intent.getStringExtra("CARTERA_ID")
        val nombreCartera = intent.getStringExtra("CARTERA_NOMBRE")

        tvSaldoCartera = findViewById(R.id.tvSaldoCartera)
        rvTransacciones = findViewById(R.id.rvGastos)
        rvTransacciones.layoutManager = LinearLayoutManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = nombreCartera?.uppercase()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        findViewById<Button>(R.id.btnAddIngreso).setOnClickListener {
            mostrarDialogoTransaccion("ingreso")
        }

        findViewById<Button>(R.id.btnAddGasto).setOnClickListener {
            mostrarDialogoTransaccion("gasto")
        }

        // Iniciamos la escucha en tiempo real apenas abre la actividad
        escucharTransaccionesEnTiempoReal()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * ESTA ES LA FUNCIÓN CLAVE: addSnapshotListener mantiene la conexión viva.
     * Si agregas un dato, Firebase le avisa a la app y esta refresca la lista solita.
     */
    private fun escucharTransaccionesEnTiempoReal() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        val cId = carteraId ?: return

        db.collection("transacciones")
            .whereEqualTo("userId", uId)
            .whereEqualTo("carteraId", cId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val lista = mutableListOf<Map<String, Any>>()
                    var tempSaldo = 0.0

                    for (doc in snapshots.documents) {
                        val data = doc.data ?: continue
                        val monto = data["monto"]?.toString()?.toDouble() ?: 0.0
                        val tipo = data["tipo"]?.toString() ?: "gasto"

                        if (tipo == "ingreso") {
                            tempSaldo += monto
                        } else {
                            tempSaldo -= monto
                        }
                        lista.add(data)
                    }

                    // Actualizamos la interfaz inmediatamente
                    saldoActualCalculado = tempSaldo
                    tvSaldoCartera.text = "$ ${String.format("%.2f", saldoActualCalculado)}"

                    // Sincronizamos el adaptador
                    rvTransacciones.adapter = GastoAdapter(lista)

                    // Sincronizamos con la colección carteras para la pantalla principal
                    db.collection("carteras").document(cId).update("balance", saldoActualCalculado)
                }
            }
    }

    private fun mostrarDialogoTransaccion(tipo: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (tipo == "ingreso") "Nuevo Ingreso" else "Nuevo Gasto")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etTitulo = EditText(this).apply { hint = "Descripción" }
        val etMonto = EditText(this).apply {
            hint = "Monto $"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        layout.addView(etTitulo)
        layout.addView(etMonto)
        builder.setView(layout)

        builder.setPositiveButton("GUARDAR") { _, _ ->
            val titulo = etTitulo.text.toString().trim()
            val montoStr = etMonto.text.toString().trim()

            if (titulo.isNotEmpty() && montoStr.isNotEmpty()) {
                val montoIngresado = montoStr.toDouble()

                if (tipo == "gasto" && montoIngresado > saldoActualCalculado) {
                    Toast.makeText(this, "FONDOS INSUFICIENTES", Toast.LENGTH_LONG).show()
                } else {
                    guardarEnFirebase(titulo, montoIngresado, tipo)
                }
            }
        }
        builder.setNegativeButton("CANCELAR", null)
        builder.show()
    }

    private fun guardarEnFirebase(titulo: String, monto: Double, tipo: String) {
        val data = hashMapOf(
            "titulo" to titulo,
            "monto" to monto,
            "tipo" to tipo,
            "fecha" to Timestamp.now(),
            "userId" to Firebase.auth.currentUser?.uid,
            "carteraId" to carteraId
        )

        // Al guardar aquí, el SnapshotListener de arriba detectará el cambio y refrescará la lista
        db.collection("transacciones").add(data)
    }
}