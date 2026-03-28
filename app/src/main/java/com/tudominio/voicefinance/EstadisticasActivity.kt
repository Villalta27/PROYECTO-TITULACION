package com.tudominio.voicefinance

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
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

class EstadisticasActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var pieChart: PieChart
    private lateinit var tvTotalMes: TextView
    private lateinit var tvMayorGasto: TextView
    private lateinit var imgEmoji: ImageView
    private lateinit var tvEstadoTitulo: TextView
    private lateinit var tvEstadoMensaje: TextView
    private lateinit var toolbar: Toolbar // La declaramos aquí para usarla en obtenerDatos

    // VARIABLES DE CONTROL PARA FILTROS
    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null
    private var registroEscucha: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas)

        // 1. Configuración de Toolbar
        toolbar = findViewById(R.id.toolbarEstadisticas)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 2. Vincular Vistas
        pieChart = findViewById(R.id.pieChartCategorias)
        tvTotalMes = findViewById(R.id.tvTotalMes)
        tvMayorGasto = findViewById(R.id.tvMayorGasto)
        imgEmoji = findViewById(R.id.imgEstadoEmoji)
        tvEstadoTitulo = findViewById(R.id.tvEstadoTitulo)
        tvEstadoMensaje = findViewById(R.id.tvEstadoMensaje)

        // 3. Botones de Filtro y Limpieza
        findViewById<ImageButton>(R.id.btnFiltroEstadisticasGeneral).setOnClickListener {
            mostrarSelectorFechas()
        }

        findViewById<ImageButton>(R.id.btnLimpiarFiltro).setOnClickListener {
            limpiarFiltros()
        }

        configurarGrafico()
        obtenerDatosGlobales()
    }

    private fun mostrarSelectorFechas() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val calI = Calendar.getInstance(); calI.set(y, m, d, 0, 0, 0); fechaInicio = calI.time

            DatePickerDialog(this, { _, y2, m2, d2 ->
                val calF = Calendar.getInstance(); calF.set(y2, m2, d2, 23, 59, 59); fechaFin = calF.time

                obtenerDatosGlobales()
                Toast.makeText(this, "Rango aplicado", Toast.LENGTH_SHORT).show()
            }, y, m, d).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun limpiarFiltros() {
        if (fechaInicio == null && fechaFin == null) {
            Toast.makeText(this, "Ya estás viendo los datos generales", Toast.LENGTH_SHORT).show()
            return
        }
        fechaInicio = null
        fechaFin = null
        obtenerDatosGlobales()
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
            setExtraOffsets(25f, 15f, 25f, 15f)
            animateY(1200)
        }
    }

    private fun obtenerDatosGlobales() {
        val uId = Firebase.auth.currentUser?.uid ?: return

        registroEscucha?.remove()

        var consulta = db.collection("transacciones").whereEqualTo("userId", uId)

        // LOGICA DE SUBTITULO Y COLOR
        if (fechaInicio != null && fechaFin != null) {
            consulta = consulta.whereGreaterThanOrEqualTo("fecha", Timestamp(fechaInicio!!))
                .whereLessThanOrEqualTo("fecha", Timestamp(fechaFin!!))

            supportActionBar?.subtitle = "Periodo Seleccionado"
            toolbar.setSubtitleTextColor(Color.parseColor("#4CC9F0")) // Cian resaltante
        } else {
            supportActionBar?.subtitle = "Historial General"
            toolbar.setSubtitleTextColor(Color.parseColor("#E0E0E0")) // Blanco grisáceo visible
        }

        registroEscucha = consulta.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener

            var totalIngresos = 0.0
            var totalGastosReales = 0.0
            var totalAhorrado = 0.0
            var montoMaxGasto = 0.0
            var nombreMaxGasto = "Ninguno"

            for (doc in snapshots) {
                val monto = doc.getDouble("monto") ?: 0.0
                val tipo = doc.getString("tipo") ?: "gasto"
                val cat = doc.getString("categoria") ?: ""
                val titulo = doc.getString("titulo") ?: "Sin título"

                when {
                    tipo == "ingreso" -> totalIngresos += monto
                    cat == "Ahorro" -> totalAhorrado += monto
                    else -> {
                        totalGastosReales += monto
                        if (monto > montoMaxGasto) {
                            montoMaxGasto = monto
                            nombreMaxGasto = titulo
                        }
                    }
                }
            }

            actualizarGraficoBalance(totalIngresos, totalGastosReales, totalAhorrado)
            calcularDesempeñoReal(totalIngresos, totalGastosReales, totalAhorrado)

            tvTotalMes.text = String.format(Locale.US, "$ %.2f", totalGastosReales)
            tvMayorGasto.text = "Gasto más alto: $nombreMaxGasto ($${String.format("%.2f", montoMaxGasto)})"
        }
    }

    private fun actualizarGraficoBalance(ingresos: Double, gastos: Double, ahorros: Double) {
        val entries = mutableListOf<PieEntry>()
        val colores = mutableListOf<Int>()

        if (ingresos <= 0 && gastos <= 0 && ahorros <= 0) {
            pieChart.clear()
            pieChart.setCenterText("SIN DATOS")
            return
        }

        if (ingresos > 0) {
            entries.add(PieEntry(ingresos.toFloat(), "Ingresos"))
            colores.add(Color.parseColor("#00FF88"))
        }
        if (gastos > 0) {
            entries.add(PieEntry(gastos.toFloat(), "Gastos"))
            colores.add(Color.parseColor("#FF4C4C"))
        }
        if (ahorros > 0) {
            entries.add(PieEntry(ahorros.toFloat(), "Ahorros"))
            colores.add(Color.parseColor("#4361EE"))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colores
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 4f
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLineColor = Color.WHITE
        dataSet.valueLinePart1Length = 0.5f

        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(com.github.mikephil.charting.formatter.DefaultValueFormatter(2))
        }
        pieChart.centerText = "BALANCE\nGENERAL"
        pieChart.invalidate()
    }

    private fun calcularDesempeñoReal(ingresos: Double, gastos: Double, ahorros: Double) {
        if (ingresos <= 0) {
            tvEstadoTitulo.text = "SIN INGRESOS"
            tvEstadoTitulo.setTextColor(Color.WHITE)
            tvEstadoMensaje.text = "No hay registros de ingresos para analizar."
            imgEmoji.setImageResource(android.R.drawable.ic_dialog_info)
            return
        }

        val porcentajeGasto = (gastos / ingresos) * 100
        val porcentajeAhorro = (ahorros / ingresos) * 100

        when {
            porcentajeGasto > 100 -> {
                tvEstadoTitulo.text = "¡TEN CUIDADO!"
                tvEstadoTitulo.setTextColor(Color.parseColor("#FF4C4C"))
                tvEstadoMensaje.text = "Tus gastos superan tus ingresos. Revisa tu presupuesto."
                imgEmoji.setImageResource(android.R.drawable.ic_dialog_alert)
            }
            porcentajeAhorro >= 20 -> {
                tvEstadoTitulo.text = "¡EXCELENTE!"
                tvEstadoTitulo.setTextColor(Color.parseColor("#00FF88"))
                tvEstadoMensaje.text = "Estás ahorrando más del 20%. ¡Sigue así!"
                imgEmoji.setImageResource(android.R.drawable.btn_star_big_on)
            }
            else -> {
                tvEstadoTitulo.text = "BUEN TRABAJO"
                tvEstadoTitulo.setTextColor(Color.parseColor("#4CC9F0"))
                tvEstadoMensaje.text = "Mantienes un balance positivo. Intenta ahorrar un poco más."
                imgEmoji.setImageResource(android.R.drawable.btn_star_big_on)
            }
        }
    }
}