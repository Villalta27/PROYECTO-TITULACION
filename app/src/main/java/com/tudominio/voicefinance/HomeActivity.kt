package com.tudominio.voicefinance

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val db = Firebase.firestore
    private lateinit var rvGastos: RecyclerView
    private lateinit var tvGastoTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvGastoTotal = findViewById(R.id.tvGastoTotal)
        rvGastos = findViewById(R.id.rvGastos)
        rvGastos.layoutManager = LinearLayoutManager(this)

        val tvFechaActual = findViewById<TextView>(R.id.tvFechaActual)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        tvFechaActual.text = sdf.format(Date()).uppercase()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_sort_by_size)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        toolbar.setNavigationOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.nav_logout) {
                Firebase.auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            drawerLayout.closeDrawers()
            true
        }

        // Iniciar la escucha en tiempo real
        escucharGastos()

        findViewById<Button>(R.id.btnAddGasto).setOnClickListener {
            mostrarOpcionesGasto()
        }
    }

    private fun escucharGastos() {
        val userId = Firebase.auth.currentUser?.uid ?: return

        db.collection("gastos")
            .whereEqualTo("userId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                val lista = mutableListOf<Map<String, Any>>()
                var totalSuma = 0.0

                if (snapshots != null) {
                    for (doc in snapshots) {
                        val data = doc.data
                        lista.add(data)
                        totalSuma += data["monto"]?.toString()?.toDouble() ?: 0.0
                    }
                }

                tvGastoTotal.text = "$ ${String.format("%.2f", totalSuma)}"
                val adapter = GastoAdapter(lista)
                rvGastos.adapter = adapter
                adapter.notifyDataSetChanged() // Notificación de cambio inmediato
            }
    }

    private fun mostrarOpcionesGasto() {
        val opciones = arrayOf("🎙️ Por Voz (Próximamente)", "✍️ Entrada Manual")
        AlertDialog.Builder(this)
            .setTitle("Seleccione método:")
            .setItems(opciones) { _, which ->
                if (which == 1) mostrarFormularioManual()
                else Toast.makeText(this, "Función de voz en desarrollo", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun mostrarFormularioManual() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_gasto, null)
        builder.setView(dialogView)

        val etTitulo = dialogView.findViewById<EditText>(R.id.etTituloGasto)
        val etMonto = dialogView.findViewById<EditText>(R.id.etMontoGasto)
        val spCategoria = dialogView.findViewById<Spinner>(R.id.spCategoria)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardarGasto)

        val categorias = arrayOf("Alimentación", "Transporte", "Vivienda", "Salud", "Educación", "Otros")
        spCategoria.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categorias)

        val dialog = builder.create()
        btnGuardar.setOnClickListener {
            val titulo = etTitulo.text.toString().trim()
            val montoStr = etMonto.text.toString().trim()
            val userId = Firebase.auth.currentUser?.uid

            if (titulo.isNotEmpty() && montoStr.isNotEmpty() && userId != null) {
                val gasto = hashMapOf(
                    "titulo" to titulo,
                    "monto" to montoStr.toDouble(),
                    "categoria" to spCategoria.selectedItem.toString(),
                    "fecha" to Timestamp.now(),
                    "userId" to userId
                )
                db.collection("gastos").add(gasto).addOnSuccessListener {
                    Toast.makeText(this, "Gasto guardado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}