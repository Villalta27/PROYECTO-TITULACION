package com.tudominio.voicefinance

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
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

        findViewById<Button>(R.id.btnAddIngreso).setOnClickListener { mostrarDialogoTransaccion("ingreso") }
        findViewById<Button>(R.id.btnAddGasto).setOnClickListener { mostrarDialogoTransaccion("gasto") }
        findViewById<Button>(R.id.btnTransferirAhorro).setOnClickListener { mostrarDialogoAhorro() }

        escucharTransaccionesEnTiempoReal()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

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
                        val data = doc.data?.toMutableMap() ?: continue
                        data["id"] = doc.id

                        val monto = data["monto"]?.toString()?.toDouble() ?: 0.0
                        val tipo = data["tipo"]?.toString() ?: "gasto"

                        if (tipo == "ingreso") tempSaldo += monto else tempSaldo -= monto
                        lista.add(data)
                    }

                    saldoActualCalculado = tempSaldo
                    tvSaldoCartera.text = "$ ${String.format("%.2f", saldoActualCalculado)}"

                    rvTransacciones.adapter = GastoAdapter(lista) { item, opcion ->
                        manejarEdicion(item, opcion)
                    }

                    db.collection("carteras").document(cId).update("balance", saldoActualCalculado)
                }
            }
    }

    private fun manejarEdicion(item: Map<String, Any>, opcion: String) {
        val idDoc = item["id"].toString()
        val tituloActual = item["titulo"].toString()

        when (opcion) {
            "Modificar Nombre" -> {
                val et = EditText(this).apply { setText(tituloActual) }
                AlertDialog.Builder(this).setTitle("EDITAR NOMBRE").setView(et)
                    .setPositiveButton("ACTUALIZAR") { _, _ ->
                        db.collection("transacciones").document(idDoc).update("titulo", et.text.toString())
                    }.setNegativeButton("CANCELAR", null).show()
            }
            "Modificar Categoría" -> {
                val categorias = arrayOf("Comida", "Transporte", "Vivienda", "Entretenimiento", "Salud", "Otros")
                val spinner = Spinner(this)
                spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categorias)

                AlertDialog.Builder(this).setTitle("SELECCIONAR CATEGORÍA").setView(spinner)
                    .setPositiveButton("CAMBIAR") { _, _ ->
                        db.collection("transacciones").document(idDoc).update("categoria", spinner.selectedItem.toString())
                    }.setNegativeButton("CANCELAR", null).show()
            }
            "Eliminar" -> {
                AlertDialog.Builder(this)
                    .setTitle("¿ELIMINAR?")
                    .setMessage("Se revertirá el saldo de tu cartera y meta de ahorro.")
                    .setPositiveButton("ELIMINAR") { _, _ -> eliminarTransaccion(item) }
                    .setNegativeButton("CANCELAR", null).show()
            }
        }
    }

    private fun eliminarTransaccion(item: Map<String, Any>) {
        val idDoc = item["id"].toString()
        val monto = item["monto"]?.toString()?.toDouble() ?: 0.0
        val metaId = item["metaId"]?.toString() ?: ""
        val fechaTrans = item["fecha"] as? Timestamp

        val batch = db.batch()

        // 1. Borrar la transacción principal
        batch.delete(db.collection("transacciones").document(idDoc))

        // 2. Si es ahorro, revertir en la meta e historial de abonos
        if (metaId.isNotEmpty()) {
            val refMeta = db.collection("metas_ahorro").document(metaId)
            batch.update(refMeta, "ahorradoActual", com.google.firebase.firestore.FieldValue.increment(-monto))

            db.collection("abonos_metas")
                .whereEqualTo("metaId", metaId)
                .whereEqualTo("fecha", fechaTrans)
                .get()
                .addOnSuccessListener { snapshots ->
                    for (doc in snapshots.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().addOnSuccessListener {
                        Toast.makeText(this, "TRANSACCIÓN ELIMINADA", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            batch.commit().addOnSuccessListener {
                Toast.makeText(this, "TRANSACCIÓN ELIMINADA", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- DIÁLOGOS DE CREACIÓN ---

    private fun mostrarDialogoAhorro() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        db.collection("metas_ahorro").whereEqualTo("userId", uId).get()
            .addOnSuccessListener { snapshots ->
                if (snapshots.isEmpty) return@addOnSuccessListener
                val nombresMetas = snapshots.map { it.getString("nombreMeta") ?: "" }.toTypedArray()
                val idsMetas = snapshots.map { it.id }.toTypedArray()

                val builder = AlertDialog.Builder(this)
                builder.setTitle("TRANSFERIR A AHORRO")
                val layout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 40, 50, 10)
                }
                val spinner = Spinner(this)
                spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresMetas)
                val etMonto = EditText(this).apply {
                    hint = "Monto $"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                layout.addView(spinner)
                layout.addView(etMonto)
                builder.setView(layout)
                builder.setPositiveButton("AHORRAR") { _, _ ->
                    val monto = etMonto.text.toString().toDoubleOrNull() ?: 0.0
                    val metaId = idsMetas[spinner.selectedItemPosition]
                    val metaNombre = nombresMetas[spinner.selectedItemPosition]
                    if (monto > 0 && monto <= saldoActualCalculado) {
                        procesarTransferenciaAhorro(metaId, metaNombre, monto)
                    }
                }
                builder.show()
            }
    }

    private fun procesarTransferenciaAhorro(metaId: String, metaNombre: String, monto: Double) {
        val fechaActual = Timestamp.now()
        val dataTransaccion = hashMapOf(
            "titulo" to "Ahorro: $metaNombre",
            "monto" to monto,
            "tipo" to "gasto",
            "categoria" to "Ahorro",
            "metaId" to metaId,
            "fecha" to fechaActual,
            "userId" to Firebase.auth.currentUser?.uid,
            "carteraId" to carteraId
        )
        val dataAbono = hashMapOf(
            "metaId" to metaId,
            "monto" to monto,
            "fecha" to fechaActual,
            "desdeCartera" to (supportActionBar?.title ?: "Cartera")
        )

        val batch = db.batch()
        batch.set(db.collection("transacciones").document(), dataTransaccion)
        batch.set(db.collection("abonos_metas").document(), dataAbono)
        batch.update(db.collection("metas_ahorro").document(metaId), "ahorradoActual", com.google.firebase.firestore.FieldValue.increment(monto))
        batch.commit()
    }

    private fun mostrarDialogoTransaccion(tipo: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (tipo == "ingreso") "NUEVO INGRESO" else "NUEVO GASTO")
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
        }
        val categorias = arrayOf("Comida", "Transporte", "Vivienda", "Entretenimiento", "Salud", "Otros")
        val spinner = Spinner(this)
        if (tipo == "gasto") {
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categorias)
            layout.addView(spinner)
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
            val cat = if (tipo == "gasto") spinner.selectedItem.toString() else "Ingreso"
            guardarEnFirebase(etTitulo.text.toString(), etMonto.text.toString().toDouble(), tipo, cat)
        }.show()
    }

    private fun guardarEnFirebase(titulo: String, monto: Double, tipo: String, categoria: String) {
        val data = hashMapOf(
            "titulo" to titulo,
            "monto" to monto,
            "tipo" to tipo,
            "categoria" to categoria,
            "fecha" to Timestamp.now(),
            "userId" to Firebase.auth.currentUser?.uid,
            "carteraId" to carteraId
        )
        db.collection("transacciones").add(data)
    }
}