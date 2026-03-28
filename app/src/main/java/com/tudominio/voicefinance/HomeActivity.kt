package com.tudominio.voicefinance

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
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

    private var registroEscucha: ListenerRegistration? = null
    private var ordenAscendente = false
    private var fechaInicioFiltro: Date? = null
    private var fechaFinFiltro: Date? = null

    // --- 1. RECEPTOR DE VOZ ---
    private val launcherVoz = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val datos = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textoEscuchado = datos?.get(0) ?: ""
            if (textoEscuchado.isNotEmpty()) {
                analizarComandoVoz(textoEscuchado)
            } else {
                Toast.makeText(this, "No logré escucharte bien. ¿Podrías repetir?", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        // Listeners Manuales
        findViewById<Button>(R.id.btnAddIngreso).setOnClickListener { mostrarDialogoTransaccion("ingreso") }
        findViewById<Button>(R.id.btnAddGasto).setOnClickListener { mostrarDialogoTransaccion("gasto") }
        findViewById<Button>(R.id.btnTransferirAhorro).setOnClickListener { mostrarDialogoAhorro() }
        findViewById<MaterialButton>(R.id.btnTransferirDinero).setOnClickListener { mostrarDialogoTransferencia() }

        // Botón Micrófono
        findViewById<MaterialButton>(R.id.btnHablarMic).setOnClickListener { iniciarEscuchaVoz() }

        findViewById<ImageButton>(R.id.btnFiltroTiempo).setOnClickListener { mostrarOpcionesDeFiltro() }

        escucharTransaccionesConFiltros()
    }

    private fun iniciarEscuchaVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime: 'Ingreso 50' o 'Gasto 20 en Comida'")
        }
        try {
            launcherVoz.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "El servicio de voz no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 2. CEREBRO ANALIZADOR MEJORADO ---
    private fun analizarComandoVoz(comando: String) {
        val texto = comando.lowercase()

        // Regex para buscar el monto
        val regexMonto = "(\\d+([.,]\\d+)?)".toRegex()
        val match = regexMonto.find(texto)
        var monto = match?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

        // Soporte para números escritos (mejorado)
        if (monto == 0.0) {
            val numerosLetras = mapOf("diez" to 10.0, "veinte" to 20.0, "treinta" to 30.0, "cuarenta" to 40.0, "cincuenta" to 50.0, "cien" to 100.0)
            for ((palabra, valor) in numerosLetras) {
                if (texto.contains(palabra)) { monto = valor; break }
            }
        }

        // --- GESTIÓN DE INCOHERENCIAS: Si no hay monto o el texto es muy corto/raro ---
        if (monto <= 0) {
            AlertDialog.Builder(this)
                .setTitle("No te entendí bien")
                .setMessage("Dijiste: \"$comando\"\n\nRecuerda mencionar un número. Por ejemplo: 'Gasto 15 en comida' o 'Ingreso 100'.")
                .setPositiveButton("Reintentar") { _, _ -> iniciarEscuchaVoz() }
                .setNegativeButton("Cerrar", null)
                .show()
            return
        }

        // 3. CLASIFICACIÓN INTELIGENTE
        val esIngreso = texto.contains("ingreso") || texto.contains("gane") || texto.contains("recibi") || texto.contains("cobré") || texto.contains("pago")
        val esTransferencia = texto.contains("transf") || texto.contains("pasa")
        val esAhorro = texto.contains("ahorr") || texto.contains("guarda")

        when {
            esTransferencia -> {
                mostrarDialogoTransferencia()
                Toast.makeText(this, "Detectada transferencia de $$monto", Toast.LENGTH_SHORT).show()
            }

            esAhorro -> {
                if (monto > saldoActualCalculado) {
                    Toast.makeText(this, "Saldo insuficiente para ahorrar $$monto", Toast.LENGTH_LONG).show()
                } else {
                    mostrarDialogoAhorro()
                }
            }

            esIngreso -> {
                guardarEnFirebase("Voz: $comando", monto, "ingreso", "Ingreso")
                Toast.makeText(this, "Ingreso de $$monto guardado correctamente", Toast.LENGTH_SHORT).show()
            }

            else -> { // Por defecto se asume GASTO si no hay palabras clave de ingreso/ahorro
                if (monto > saldoActualCalculado) {
                    Toast.makeText(this, "SALDO INSUFICIENTE. Tienes $${String.format("%.2f", saldoActualCalculado)}", Toast.LENGTH_LONG).show()
                } else {
                    obtenerCategoriasUsuario { listaCats ->
                        var catFinal = listaCats.find { texto.contains(it.lowercase()) }
                        if (catFinal == null) {
                            catFinal = "Otros"
                            if (!listaCats.any { it.equals("Otros", true) }) crearCategoriaOtrosAuto()
                        }
                        guardarEnFirebase("Voz: $comando", monto, "gasto", catFinal!!)
                        Toast.makeText(this, "Gasto de $$monto registrado en $catFinal", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun crearCategoriaOtrosAuto() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uId).collection("categorias_personalizadas")
            .document("otros_fijo").set(hashMapOf("nombre" to "Otros"))
    }

    // ==========================================
    // LÓGICA DE FIREBASE Y MÉTODOS ORIGINALES
    // ==========================================

    private fun mostrarDialogoTransferencia() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        db.collection("carteras").whereEqualTo("userId", uId).get().addOnSuccessListener { snapshots ->
            if (snapshots.size() < 2) {
                Toast.makeText(this, "Necesitas al menos 2 carteras", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val vista = layoutInflater.inflate(R.layout.dialog_transferencia, null)
            val etMonto = vista.findViewById<EditText>(R.id.etMontoTransferir)
            val spinnerDestino = vista.findViewById<Spinner>(R.id.spinnerCarterasDestino)
            val otrasCarteras = snapshots.documents.filter { it.id != carteraId }
            val nombres = otrasCarteras.map { it.getString("nombre") ?: "Sin nombre" }
            spinnerDestino.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)

            AlertDialog.Builder(this).setView(vista).setPositiveButton("TRANSFERIR") { _, _ ->
                val m = etMonto.text.toString().toDoubleOrNull() ?: 0.0
                if (m > 0 && m <= saldoActualCalculado) ejecutarTransferencia(otrasCarteras[spinnerDestino.selectedItemPosition].id, nombres[spinnerDestino.selectedItemPosition], m)
                else Toast.makeText(this, "Saldo insuficiente o monto inválido", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("CANCELAR", null).show()
        }
    }

    private fun ejecutarTransferencia(idDestino: String, nombreDestino: String, monto: Double) {
        val uId = Firebase.auth.currentUser?.uid ?: return
        val batch = db.batch()
        val refSalida = db.collection("transacciones").document()
        batch.set(refSalida, hashMapOf("titulo" to "Transferencia a $nombreDestino", "monto" to monto, "tipo" to "gasto", "categoria" to "Ajuste", "fecha" to Timestamp.now(), "userId" to uId, "carteraId" to carteraId))
        val refEntrada = db.collection("transacciones").document()
        batch.set(refEntrada, hashMapOf("titulo" to "Recibido de otra cartera", "monto" to monto, "tipo" to "ingreso", "categoria" to "Ajuste", "fecha" to Timestamp.now(), "userId" to uId, "carteraId" to idDestino))
        batch.commit().addOnSuccessListener { Toast.makeText(this, "Transferencia exitosa", Toast.LENGTH_SHORT).show() }
    }

    private fun obtenerCategoriasUsuario(callback: (List<String>) -> Unit) {
        val uId = Firebase.auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uId).collection("categorias_personalizadas").get().addOnSuccessListener { snapshots ->
            val lista = snapshots.documents.map { it.getString("nombre") ?: "" }
            if (lista.isEmpty()) callback(listOf("Comida", "Transporte", "Vivienda", "Salud", "Otros"))
            else callback(lista)
        }.addOnFailureListener { callback(listOf("Otros")) }
    }

    private fun mostrarDialogoTransaccion(tipo: String) {
        obtenerCategoriasUsuario { lista ->
            val builder = AlertDialog.Builder(this)
            val vista = layoutInflater.inflate(R.layout.dialog_add_gasto, null)
            val etMonto = vista.findViewById<EditText>(R.id.etMontoGasto)
            val spinner = vista.findViewById<Spinner>(R.id.spCategoria)
            if (tipo == "gasto") spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lista)
            else spinner.visibility = View.GONE

            builder.setView(vista).setPositiveButton("Guardar") { _, _ ->
                val m = etMonto.text.toString().toDoubleOrNull() ?: 0.0
                if (m > 0) {
                    if (tipo == "gasto" && m > saldoActualCalculado) Toast.makeText(this, "SALDO INSUFICIENTE", Toast.LENGTH_SHORT).show()
                    else guardarEnFirebase("Manual", m, tipo, if (tipo == "gasto") spinner.selectedItem.toString() else "Ingreso")
                }
            }.show()
        }
    }

    private fun escucharTransaccionesConFiltros() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        val cId = carteraId ?: return
        registroEscucha?.remove()
        val dir = if (ordenAscendente) Query.Direction.ASCENDING else Query.Direction.DESCENDING
        var consulta = db.collection("transacciones").whereEqualTo("userId", uId).whereEqualTo("carteraId", cId)
        if (fechaInicioFiltro != null && fechaFinFiltro != null) {
            consulta = consulta.whereGreaterThanOrEqualTo("fecha", Timestamp(fechaInicioFiltro!!)).whereLessThanOrEqualTo("fecha", Timestamp(fechaFinFiltro!!))
        }
        registroEscucha = consulta.orderBy("fecha", dir).addSnapshotListener { snapshots, _ ->
            if (snapshots != null) {
                val lista = mutableListOf<Map<String, Any>>()
                var tempSaldo = 0.0
                for (doc in snapshots.documents) {
                    val data = doc.data?.toMutableMap() ?: continue
                    data["id"] = doc.id
                    val m = data["monto"]?.toString()?.toDouble() ?: 0.0
                    if (data["tipo"] == "ingreso") tempSaldo += m else tempSaldo -= m
                    lista.add(data)
                }
                saldoActualCalculado = tempSaldo
                tvSaldoCartera.text = "$ ${String.format("%.2f", saldoActualCalculado)}"
                rvTransacciones.adapter = GastoAdapter(lista) { item, op -> manejarEdicion(item, op) }
                db.collection("carteras").document(cId).update("balance", saldoActualCalculado)
            }
        }
    }

    private fun guardarEnFirebase(titulo: String, monto: Double, tipo: String, categoria: String) {
        val uId = Firebase.auth.currentUser?.uid ?: return
        val data = hashMapOf("titulo" to titulo, "monto" to monto, "tipo" to tipo, "categoria" to categoria, "fecha" to Timestamp.now(), "userId" to uId, "carteraId" to carteraId)
        db.collection("transacciones").add(data)
    }

    private fun eliminarTransaccion(item: Map<String, Any>) {
        val idDoc = item["id"].toString()
        val monto = item["monto"]?.toString()?.toDouble() ?: 0.0
        val metaId = item["metaId"]?.toString() ?: ""
        val batch = db.batch()
        batch.delete(db.collection("transacciones").document(idDoc))
        if (metaId.isNotEmpty()) batch.update(db.collection("metas_ahorro").document(metaId), "ahorradoActual", com.google.firebase.firestore.FieldValue.increment(-monto))
        batch.commit()
    }

    private fun mostrarDialogoAhorro() {
        val uId = Firebase.auth.currentUser?.uid ?: return
        db.collection("metas_ahorro").whereEqualTo("userId", uId).get().addOnSuccessListener { snapshots ->
            if (snapshots.isEmpty) return@addOnSuccessListener
            val nombres = snapshots.map { it.getString("nombreMeta") ?: "" }.toTypedArray()
            val ids = snapshots.map { it.id }.toTypedArray()
            val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 40, 50, 10) }
            val spinner = Spinner(this); spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)
            val et = EditText(this).apply { hint = "Monto $"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
            layout.addView(spinner); layout.addView(et)
            AlertDialog.Builder(this).setTitle("Ahorrar").setView(layout).setPositiveButton("OK") { _, _ ->
                val mVal = et.text.toString().toDoubleOrNull() ?: 0.0
                if (mVal > saldoActualCalculado) Toast.makeText(this, "SALDO INSUFICIENTE", Toast.LENGTH_SHORT).show()
                else if (mVal > 0) procesarTransferenciaAhorro(ids[spinner.selectedItemPosition], nombres[spinner.selectedItemPosition], mVal)
            }.show()
        }
    }

    private fun procesarTransferenciaAhorro(metaId: String, metaNombre: String, monto: Double) {
        val batch = db.batch()
        batch.set(db.collection("transacciones").document(), hashMapOf("titulo" to "Ahorro: $metaNombre", "monto" to monto, "tipo" to "gasto", "categoria" to "Ahorro", "metaId" to metaId, "fecha" to Timestamp.now(), "userId" to Firebase.auth.currentUser?.uid, "carteraId" to carteraId))
        batch.update(db.collection("metas_ahorro").document(metaId), "ahorradoActual", com.google.firebase.firestore.FieldValue.increment(monto))
        batch.commit()
    }

    private fun manejarEdicion(item: Map<String, Any>, opcion: String) {
        if (opcion == "Eliminar") eliminarTransaccion(item)
    }

    private fun mostrarOpcionesDeFiltro() {
        val op = arrayOf(if (ordenAscendente) "Recientes" else "Antiguos", "Rango", "Limpiar")
        AlertDialog.Builder(this).setItems(op) { _, w ->
            if (w == 0) { ordenAscendente = !ordenAscendente; escucharTransaccionesConFiltros() }
            if (w == 1) mostrarSelectorDeFechas()
            if (w == 2) { fechaInicioFiltro = null; fechaFinFiltro = null; escucharTransaccionesConFiltros() }
        }.show()
    }

    private fun mostrarSelectorDeFechas() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val calI = Calendar.getInstance(); calI.set(y, m, d, 0, 0, 0); fechaInicioFiltro = calI.time
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val calF = Calendar.getInstance(); calF.set(y2, m2, d2, 23, 59, 59); fechaFinFiltro = calF.time
                escucharTransaccionesConFiltros()
            }, y, m, d).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}