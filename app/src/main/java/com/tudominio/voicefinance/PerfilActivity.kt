package com.tudominio.voicefinance

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class PerfilActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage.reference
    private lateinit var ivFotoPerfil: ImageView

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { subirFotoAFirebase(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) galleryLauncher.launch("image/*")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Toolbar con icono nativo para evitar el error de ic_arrow_back
        val toolbar = findViewById<Toolbar>(R.id.toolbarPerfil)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_revert)

        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        val etNombre = findViewById<EditText>(R.id.etNombreUsuarioPerfil)
        val btnNombre = findViewById<Button>(R.id.btnActualizarNombre)

        val userId = auth.currentUser?.uid

        userId?.let { uid ->
            db.collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etNombre.setText(doc.getString("nombre"))
                    val url = doc.getString("fotoUrl")
                    if (!url.isNullOrEmpty()) Glide.with(this).load(url).circleCrop().into(ivFotoPerfil)
                }
            }
        }

        ivFotoPerfil.setOnClickListener { verificarPermisos() }

        btnNombre.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            if (nombre.isNotEmpty() && userId != null) {
                db.collection("usuarios").document(userId)
                    .set(mapOf("nombre" to nombre), SetOptions.merge())
                    .addOnSuccessListener { Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun verificarPermisos() {
        val permiso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED) galleryLauncher.launch("image/*") else requestPermissionLauncher.launch(permiso)
    }

    private fun subirFotoAFirebase(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val ref = storage.child("perfiles/$userId.jpg")

        Toast.makeText(this, "Subiendo...", Toast.LENGTH_SHORT).show()

        // putFile inicia la subida.addOnSuccessListener espera a que TERMINE antes de pedir la URL
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url ->
                val link = url.toString()
                db.collection("usuarios").document(userId).set(mapOf("fotoUrl" to link), SetOptions.merge())
                    .addOnSuccessListener {
                        Glide.with(this@PerfilActivity).load(link).circleCrop().into(ivFotoPerfil)
                        Toast.makeText(this, "¡Listo!", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}