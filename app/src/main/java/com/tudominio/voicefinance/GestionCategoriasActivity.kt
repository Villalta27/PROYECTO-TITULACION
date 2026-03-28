package com.tudominio.voicefinance

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GestionCategoriasActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvCategorias: RecyclerView
    private val listaCategorias = mutableListOf<Map<String, String>>()
    private lateinit var adapter: CategoriasConfigAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_categorias)

        val toolbar = findViewById<Toolbar>(R.id.toolbarGestionCategorias)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rvCategorias = findViewById(R.id.rvCategoriasConfig)
        rvCategorias.layoutManager = LinearLayoutManager(this)

        adapter = CategoriasConfigAdapter(listaCategorias,
            onDelete = { id -> eliminarCategoria(id) },
            onEdit = { id, nombreActual -> mostrarDialogoCategoria(id, nombreActual) }
        )
        rvCategorias.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAgregarCategoria).setOnClickListener {
            if (listaCategorias.size >= 8) {
                Toast.makeText(this, "Límite alcanzado (Máx 8 categorías)", Toast.LENGTH_SHORT).show()
            } else {
                mostrarDialogoCategoria()
            }
        }

        escucharCategorias()
    }

    private fun escucharCategorias() {
        val uId = Firebase.auth.currentUser?.uid ?: return

        db.collection("usuarios").document(uId).collection("categorias_personalizadas")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    // --- CAMBIO CLAVE: SI ESTÁ VACÍO, CREAMOS LAS BÁSICAS ---
                    if (snapshots.isEmpty) {
                        crearCategoriasPorDefecto(uId)
                    } else {
                        listaCategorias.clear()
                        for (doc in snapshots.documents) {
                            val nombre = doc.getString("nombre") ?: ""
                            listaCategorias.add(mapOf("id" to doc.id, "nombre" to nombre))
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }
    }

    // Nueva función para que la pantalla no aparezca vacía la primera vez
    private fun crearCategoriasPorDefecto(uId: String) {
        val basicas = listOf("Comida", "Transporte", "Vivienda", "Salud", "Otros")
        val batch = db.batch()

        for (nombreCat in basicas) {
            val docRef = db.collection("usuarios").document(uId)
                .collection("categorias_personalizadas").document()
            batch.set(docRef, mapOf("nombre" to nombreCat))
        }

        batch.commit().addOnSuccessListener {
            // El listener detectará el cambio y actualizará la lista solo
        }
    }

    private fun mostrarDialogoCategoria(id: String? = null, nombreActual: String = "") {
        val etNombre = EditText(this).apply {
            setText(nombreActual)
            hint = "Ej: Gimnasio, Mascotas..."
            setSelection(this.text.length)
        }

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(60, 40, 60, 10)
        etNombre.layoutParams = params
        container.addView(etNombre)

        val titulo = if (id == null) "NUEVA CATEGORÍA" else "EDITAR CATEGORÍA"

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setView(container)
            .setPositiveButton("GUARDAR") { _, _ ->
                val nuevoNombre = etNombre.text.toString().trim()
                if (nuevoNombre.isEmpty()) return@setPositiveButton

                val existe = listaCategorias.any {
                    it["nombre"].equals(nuevoNombre, ignoreCase = true) && it["id"] != id
                }

                if (existe) {
                    Toast.makeText(this, "Esta categoría ya existe", Toast.LENGTH_SHORT).show()
                } else {
                    guardarEnFirebase(id, nuevoNombre)
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun guardarEnFirebase(id: String?, nombre: String) {
        val uId = Firebase.auth.currentUser?.uid ?: return
        val docRef = if (id == null) {
            db.collection("usuarios").document(uId).collection("categorias_personalizadas").document()
        } else {
            db.collection("usuarios").document(uId).collection("categorias_personalizadas").document(id)
        }

        docRef.set(mapOf("nombre" to nombre))
    }

    private fun eliminarCategoria(id: String) {
        val uId = Firebase.auth.currentUser?.uid ?: return
        AlertDialog.Builder(this)
            .setTitle("¿ELIMINAR?")
            .setMessage("¿Deseas quitar esta categoría?")
            .setPositiveButton("SÍ") { _, _ ->
                db.collection("usuarios").document(uId).collection("categorias_personalizadas").document(id)
                    .delete()
            }
            .setNegativeButton("NO", null)
            .show()
    }
}