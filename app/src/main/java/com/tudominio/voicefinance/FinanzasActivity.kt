package com.tudominio.voicefinance

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class FinanzasActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvFinanzas: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var adapter: FinanzaAdapter
    private val listaCarteras = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finanzas)

        val toolbar = findViewById<Toolbar>(R.id.toolbarFinanzas)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout_finanzas)
        val navView = findViewById<NavigationView>(R.id.nav_view_finanzas)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_sort_by_size)

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_user -> startActivity(Intent(this, PerfilActivity::class.java))
                R.id.nav_history -> startActivity(Intent(this, HistorialCarterasActivity::class.java))
                R.id.nav_savings -> startActivity(Intent(this, MetasAhorroActivity::class.java))
                R.id.nav_stats -> startActivity(Intent(this, SeleccionEstadisticasActivity::class.java))
                R.id.nav_config -> startActivity(Intent(this, ConfiguracionActivity::class.java))
                R.id.nav_logout -> {
                    Firebase.auth.signOut()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        rvFinanzas = findViewById(R.id.rvFinanzas)
        rvFinanzas.layoutManager = LinearLayoutManager(this)

        // --- ADAPTADOR CON EL NUEVO PARÁMETRO DE MODO LECTURA (FALSE) ---
        adapter = FinanzaAdapter(listaCarteras, false,
            onItemClick = { id, nombre -> abrirHome(id, nombre) },
            onOpcionesClick = { id, nombre, view -> mostrarMenuOpciones(id, nombre, view) }
        )
        rvFinanzas.adapter = adapter

        findViewById<ExtendedFloatingActionButton>(R.id.btnAgregarFinanza).setOnClickListener {
            mostrarDialogoCrear()
        }

        escucharCarteras()
        actualizarHeaderMenu()
    }

    private fun mostrarMenuOpciones(id: String, nombre: String, view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Cambiar nombre")
        popup.menu.add("Eliminar cartera")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Cambiar nombre" -> editarNombre(id, nombre)
                "Eliminar cartera" -> validarSaldoAntesDeEliminar(id, nombre)
            }
            true
        }
        popup.show()
    }

    // --- LÓGICA DE ELIMINACIÓN CON TRASPASO DE SALDO ---
    private fun validarSaldoAntesDeEliminar(idCartera: String, nombre: String) {
        val carteraActual = listaCarteras.find { it["id"] == idCartera }
        val saldo = carteraActual?.get("balance")?.toString()?.toDoubleOrNull() ?: 0.0

        val builder = AlertDialog.Builder(this)
        builder.setTitle("¿ELIMINAR $nombre?")

        if (saldo == 0.0) {
            builder.setMessage("¿Estás seguro? Esta cartera no tiene fondos.")
            builder.setPositiveButton("ELIMINAR") { _, _ -> eliminarCarteraDirecto(idCartera) }
        } else {
            builder.setMessage("Esta cartera tiene $${String.format("%.2f", saldo)}. \n\nPara eliminarla, debes mover este dinero a otra de tus carteras.")
            builder.setPositiveButton("TRASPASAR Y ELIMINAR") { _, _ ->
                mostrarSelectorDeDestino(idCartera, nombre, saldo)
            }
        }
        builder.setNegativeButton("CANCELAR", null)
        builder.show()
    }

    private fun mostrarSelectorDeDestino(idOrigen: String, nombreOrigen: String, monto: Double) {
        val otrasCarteras = listaCarteras.filter { it["id"] != idOrigen }

        if (otrasCarteras.isEmpty()) {
            Toast.makeText(this, "Crea otra cartera para poder mover el dinero primero", Toast.LENGTH_LONG).show()
            return
        }

        val nombres = otrasCarteras.map { it["nombre"].toString() }.toTypedArray()
        val ids = otrasCarteras.map { it["id"].toString() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("DESTINO DEL DINERO")
            .setItems(nombres) { _, i ->
                ejecutarCierreConTraspaso(idOrigen, nombreOrigen, ids[i], nombres[i], monto)
            }
            .show()
    }

    private fun ejecutarCierreConTraspaso(idOri: String, nomOri: String, idDes: String, nomDes: String, monto: Double) {
        val uId = Firebase.auth.currentUser?.uid ?: return
        val batch = db.batch()

        // 1. Crear transacción de ajuste en la nueva cartera
        val refNuevaTrans = db.collection("transacciones").document()
        val dataTrans = hashMapOf(
            "titulo" to "Traspaso por cierre de: $nomOri",
            "monto" to monto,
            "tipo" to "ingreso",
            "categoria" to "Ajuste",
            "fecha" to Timestamp.now(),
            "userId" to uId,
            "carteraId" to idDes
        )
        batch.set(refNuevaTrans, dataTrans)

        // 2. Incrementar el balance de la cartera destino
        batch.update(db.collection("carteras").document(idDes), "balance", FieldValue.increment(monto))

        // 3. Eliminar la cartera origen
        batch.delete(db.collection("carteras").document(idOri))

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Fondos movidos a $nomDes. Cartera eliminada.", Toast.LENGTH_LONG).show()
        }
    }

    private fun eliminarCarteraDirecto(id: String) {
        db.collection("carteras").document(id).delete()
            .addOnSuccessListener { Toast.makeText(this, "Cartera eliminada", Toast.LENGTH_SHORT).show() }
    }

    // --- FUNCIONES RESTANTES (MANTENIDAS) ---

    private fun escucharCarteras() {
        val userId = Firebase.auth.currentUser?.uid ?: return
        db.collection("carteras").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    listaCarteras.clear()
                    for (doc in snapshots.documents) {
                        val map = doc.data?.toMutableMap() ?: mutableMapOf()
                        map["id"] = doc.id
                        listaCarteras.add(map)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun actualizarHeaderMenu() {
        val navView = findViewById<NavigationView>(R.id.nav_view_finanzas)
        val headerView = navView.getHeaderView(0)
        val tvNombre = headerView.findViewById<TextView>(R.id.tvNombreUsuarioHeader)
        val tvEmail = headerView.findViewById<TextView>(R.id.tvUserEmailHeader)
        val ivAvatar = headerView.findViewById<ImageView>(R.id.imageView)

        val user = Firebase.auth.currentUser
        tvEmail.text = user?.email
        user?.uid?.let { uid ->
            db.collection("usuarios").document(uid).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    tvNombre.text = snapshot.getString("nombre")?.uppercase() ?: "USUARIO"
                    val fotoUrl = snapshot.getString("fotoUrl")
                    if (!fotoUrl.isNullOrEmpty()) {
                        Glide.with(this).load(fotoUrl).circleCrop().into(ivAvatar)
                    }
                }
            }
        }
    }

    private fun editarNombre(id: String, actual: String) {
        val et = EditText(this).apply { setText(actual) }
        AlertDialog.Builder(this).setTitle("Editar Nombre").setView(et)
            .setPositiveButton("Guardar") { _, _ ->
                db.collection("carteras").document(id).update("nombre", et.text.toString())
            }.show()
    }

    private fun abrirHome(id: String, nombre: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("CARTERA_ID", id)
        intent.putExtra("CARTERA_NOMBRE", nombre)
        startActivity(intent)
    }

    private fun mostrarDialogoCrear() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("Nueva Cartera").setView(input)
            .setPositiveButton("CREAR") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val userId = Firebase.auth.currentUser?.uid ?: return@setPositiveButton
                    val fecha = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
                    val data = hashMapOf("nombre" to nombre, "userId" to userId, "balance" to 0.0, "fechaCreacion" to fecha)
                    db.collection("carteras").add(data)
                }
            }.show()
    }
}