package com.tudominio.voicefinance

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class DetalleEstadisticaCarteraActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var pieChart: PieChart
    private lateinit var toolbar: Toolbar
    private lateinit var tvPieNota: TextView
    private var carteraId: String? = null

    // VARIABLES DE CONTROL PARA FILTROS
    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null
    private var registroEscucha: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_estadistica_cartera)

        carteraId = intent.getStringExtra("CARTERA_ID")
        val nombre = intent.getStringExtra("CARTERA_NOMBRE") ?: "Cartera"

        // 1. Configuración de Toolbar
        toolbar = findViewById(R.id.toolbarDetalleCartera)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Balance: $nombre"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 2. Vincular Vistas
        pieChart = findViewById(R.id.pieChartCartera)
        tvPieNota = findViewById(R.id.tvPieNota)

        // 3. Botones de Filtro (Asegúrate de que los IDs coincidan con el XML que modificamos)
        findViewById<ImageButton>(R.id.btnFiltroCarteraDetalle).setOnClickListener {
            mostrarSelectorFechas()
        }

        findViewById<ImageButton>(R.id.btnLimpiarFiltroCartera).setOnClickListener {
            limpiarFiltros()
        }

        configurarGrafico()

        // Llamada inicial
        carteraId?.let { cargarDatosGrafico(it) }
    }

    private fun mostrarSelectorFechas() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val calI = Calendar.getInstance(); calI.set(y, m, d, 0, 0, 0); fechaInicio = calI.time

            DatePickerDialog(this, { _, y2, m2, d2 ->
                val calF = Calendar.getInstance(); calF.set(y2, m2, d2, 23, 59, 59); fechaFin = calF.time

                carteraId?.let { cargarDatosGrafico(it) }
                Toast.makeText(this, "Rango aplicado", Toast.LENGTH_SHORT).show()
            }, y, m, d).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun limpiarFiltros() {
        if (fechaInicio == null && fechaFin == null) {
            Toast.makeText(this, "Ya ves el historial total", Toast.LENGTH_SHORT).show()
            return
        }
        fechaInicio = null
        fechaFin = null
        carteraId?.let { cargarDatosGrafico(it) }
        Toast.makeText(this, "Filtros eliminados", Toast.LENGTH_SHORT).show()
    }

    private fun configurarGrafico() {
        pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setCenterTextColor(Color.WHITE)
            setEntryLabelColor(Color.WHITE)
            legend.textColor = Color.WHITE
            setExtraOffsets(20f, 10f, 20f, 10f)
            animateY(1000)
        }
    }

    private fun cargarDatosGrafico(cId: String) {
        val uId = Firebase.auth.currentUser?.uid ?: return

        // Limpiamos escucha previa para evitar duplicados
        registroEscucha?.remove()

        var consulta = db.collection("transacciones")
            .whereEqualTo("userId", uId)
            .whereEqualTo("carteraId", cId)

        // Lógica de Subtítulo y Filtro de Fecha
        if (fechaInicio != null && fechaFin != null) {
            consulta = consulta.whereGreaterThanOrEqualTo("fecha", Timestamp(fechaInicio!!))
                .whereLessThanOrEqualTo("fecha", Timestamp(fechaFin!!))

            supportActionBar?.subtitle = "Periodo Seleccionado"
            toolbar.setSubtitleTextColor(Color.parseColor("#4CC9F0")) // Cian notorio
            tvPieNota.text = "Los datos mostrados corresponden al periodo seleccionado."
        } else {
            supportActionBar?.subtitle = "Historial Total"
            toolbar.setSubtitleTextColor(Color.parseColor("#E0E0E0")) // Blanco grisáceo visible
            tvPieNota.text = "Los datos mostrados corresponden al historial total de esta cartera."
        }

        // Usamos addSnapshotListener para que sea en tiempo real
        registroEscucha = consulta.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener

            var ingresos = 0.0
            var gastos = 0.0
            var ahorros = 0.0

            for (doc in snapshots) {
                val monto = doc.getDouble("monto") ?: 0.0
                val tipo = doc.getString("tipo") ?: ""
                val cat = doc.getString("categoria") ?: ""

                if (cat == "Ahorro") {
                    ahorros += monto
                } else if (tipo == "ingreso") {
                    ingresos += monto
                } else {
                    gastos += monto
                }
            }

            mostrarGrafico(ingresos, gastos, ahorros)
        }
    }

    private fun mostrarGrafico(ing: Double, gas: Double, aho: Double) {
        val entries = mutableListOf<PieEntry>()
        val colores = mutableListOf<Int>()

        if (ing <= 0 && gas <= 0 && aho <= 0) {
            pieChart.clear()
            pieChart.centerText = "Sin movimientos"
            pieChart.invalidate()
            return
        }

        if (ing > 0) {
            entries.add(PieEntry(ing.toFloat(), "Ingresos"))
            colores.add(Color.parseColor("#00FF88"))
        }
        if (gas > 0) {
            entries.add(PieEntry(gas.toFloat(), "Gastos"))
            colores.add(Color.parseColor("#E63946"))
        }
        if (aho > 0) {
            entries.add(PieEntry(aho.toFloat(), "Ahorros"))
            colores.add(Color.parseColor("#4361EE"))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colores
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 3f

        val data = PieData(dataSet).apply {
            setValueFormatter(com.github.mikephil.charting.formatter.DefaultValueFormatter(2))
        }

        pieChart.data = data
        pieChart.centerText = "Balance"
        pieChart.invalidate()
    }
}