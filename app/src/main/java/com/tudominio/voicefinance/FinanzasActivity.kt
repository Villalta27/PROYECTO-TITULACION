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
import com.google.firebase.auth.ktx.auth
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

        // 1. Configuración de Toolbar y Drawer (Menú Lateral)
        val toolbar = findViewById<Toolbar>(R.id.toolbarFinanzas)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout_finanzas)
        val navView = findViewById<NavigationView>(R.id.nav_view_finanzas)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_sort_by_size)

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 2. Navegación del Menú
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_user -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                }
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

        // 3. Configuración del RecyclerView (Lista de Carteras)
        rvFinanzas = findViewById(R.id.rvFinanzas)
        rvFinanzas.layoutManager = LinearLayoutManager(this)

        adapter = FinanzaAdapter(listaCarteras,
            onItemClick = { id, nombre -> abrirHome(id, nombre) },
            onOpcionesClick = { id, nombre, view -> mostrarMenuOpciones(id, nombre, view) }
        )
        rvFinanzas.adapter = adapter

        // 4. Botón para agregar carteras
        findViewById<ExtendedFloatingActionButton>(R.id.btnAgregarFinanza).setOnClickListener {
            mostrarDialogoCrear()
        }

        // Inicializar datos y sincronizar Perfil
        escucharCarteras()
        actualizarHeaderMenu()
    }

    // FUNCIÓN CRÍTICA: Sincroniza Foto y Nombre en el Header
    private fun actualizarHeaderMenu() {
        val navView = findViewById<NavigationView>(R.id.nav_view_finanzas)
        val headerView = navView.getHeaderView(0)

        val tvNombre = headerView.findViewById<TextView>(R.id.tvNombreUsuarioHeader)
        val tvEmail = headerView.findViewById<TextView>(R.id.tvUserEmailHeader)
        val ivAvatar = headerView.findViewById<ImageView>(R.id.imageView) // Tu ID original

        val user = Firebase.auth.currentUser
        tvEmail.text = user?.email

        user?.uid?.let { uid ->
            // Escucha cambios en tiempo real (SnapshotListener)
            db.collection("usuarios").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        // Actualiza Nombre
                        val nombre = snapshot.getString("nombre")
                        tvNombre.text = nombre?.uppercase() ?: "USUARIO"

                        // Actualiza Foto con Glide usando tu ic_user como respaldo
                        val fotoUrl = snapshot.getString("fotoUrl")
                        if (!fotoUrl.isNullOrEmpty()) {
                            Glide.with(this@FinanzasActivity)
                                .load(fotoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_user)
                                .into(ivAvatar)
                        } else {
                            ivAvatar.setImageResource(R.drawable.ic_user)
                        }
                    }
                }
        }
    }

    private fun escucharCarteras() {
        val userId = Firebase.auth.currentUser?.uid ?: return
        db.collection("carteras")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
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

    private fun mostrarMenuOpciones(id: String, nombre: String, view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Cambiar nombre")
        popup.menu.add("Eliminar cartera")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Cambiar nombre" -> editarNombre(id, nombre)
                "Eliminar cartera" -> eliminarCarteraEnCascada(id)
            }
            true
        }
        popup.show()
    }

    private fun eliminarCarteraEnCascada(idCartera: String) {
        db.collection("transacciones")
            .whereEqualTo("carteraId", idCartera)
            .get()
            .addOnSuccessListener { snapshots ->
                val batch = db.batch()
                for (doc in snapshots) { batch.delete(doc.reference) }
                batch.delete(db.collection("carteras").document(idCartera))
                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "Cartera eliminada", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun mostrarDialogoCrear() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("Nueva Cartera").setView(input)
            .setPositiveButton("CREAR") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val userId = Firebase.auth.currentUser?.uid ?: return@setPositiveButton
                    val fecha = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
                    val data = hashMapOf(
                        "nombre" to nombre,
                        "userId" to userId,
                        "balance" to 0.0,
                        "fechaCreacion" to fecha
                    )
                    db.collection("carteras").add(data)
                }
            }.show()
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
}