package com.tudominio.voicefinance

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MetasAhorroActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvMetas: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnFab: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metas_ahorro)

        // 1. Configuración de Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbarMetas)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 2. Referencias de la UI
        rvMetas = findViewById(R.id.rvMetas)
        rvMetas.layoutManager = LinearLayoutManager(this)
        layoutEmpty = findViewById(R.id.layoutEmptyState)
        btnFab = findViewById(R.id.btnAgregarMetaAhorro)
        val btnGrande = findViewById<Button>(R.id.btnGrandeAgregar)

        // 3. Ambos botones disparan la misma función
        btnFab.setOnClickListener { mostrarDialogoNuevaMeta() }
        btnGrande.setOnClickListener { mostrarDialogoNuevaMeta() }

        // 4. Escucha activa de Firebase
        escucharMetasRealTime()
    }

    private fun escucharMetasRealTime() {
        val userId = Firebase.auth.currentUser?.uid ?: return

        db.collection("metas_ahorro")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    if (snapshots.isEmpty) {
                        // ESTADO VACÍO: Mostramos el botón grande central rojo
                        layoutEmpty.visibility = View.VISIBLE
                        btnFab.visibility = View.GONE
                        rvMetas.visibility = View.GONE
                    } else {
                        // YA HAY METAS: Mostramos lista y el botón FAB abajo a la derecha
                        layoutEmpty.visibility = View.GONE
                        btnFab.visibility = View.VISIBLE
                        rvMetas.visibility = View.VISIBLE

                        val listaMetas = mutableListOf<Map<String, Any>>()
                        for (doc in snapshots.documents) {
                            val data = doc.data?.toMutableMap() ?: mutableMapOf()
                            data["id"] = doc.id
                            listaMetas.add(data)
                        }

                        // CONECTAMOS EL ADAPTADOR
                        rvMetas.adapter = MetaAdapter(listaMetas)
                    }
                }
            }
    }

    private fun mostrarDialogoNuevaMeta() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("NUEVA META DE AHORRO")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
        }

        val etNombre = EditText(this).apply { hint = "¿Para qué quieres ahorrar?" }
        val etMonto = EditText(this).apply {
            hint = "Monto objetivo $"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        layout.addView(etNombre)
        layout.addView(etMonto)
        builder.setView(layout)

        builder.setPositiveButton("CREAR") { _, _ ->
            val nombre = etNombre.text.toString().trim()
            val montoStr = etMonto.text.toString().trim()

            if (nombre.isNotEmpty() && montoStr.isNotEmpty()) {
                guardarMetaEnFirebase(nombre, montoStr.toDouble())
            } else {
                Toast.makeText(this, "Completa los datos", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("CANCELAR", null)
        builder.show()
    }

    private fun guardarMetaEnFirebase(nombre: String, objetivo: Double) {
        val userId = Firebase.auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "nombreMeta" to nombre,
            "objetivo" to objetivo,
            "ahorradoActual" to 0.0,
            "userId" to userId
        )

        db.collection("metas_ahorro").add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Meta creada!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar meta", Toast.LENGTH_SHORT).show()
            }
    }
}