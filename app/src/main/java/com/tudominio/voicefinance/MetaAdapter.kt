package com.tudominio.voicefinance

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MetaAdapter(private val lista: List<Map<String, Any>>) :
    RecyclerView.Adapter<MetaAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tvNombreMetaItem)
        val ahorrado: TextView = v.findViewById(R.id.tvAhorradoItem)
        val objetivo: TextView = v.findViewById(R.id.tvObjetivoItem)
        val barra: ProgressBar = v.findViewById(R.id.progressMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_meta, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meta = lista[position]

        // Extraemos los datos con seguridad
        val idMeta = meta["id"]?.toString() ?: ""
        val nombreMeta = meta["nombreMeta"]?.toString() ?: "Sin nombre"
        val montoObjetivo = meta["objetivo"]?.toString()?.toDoubleOrNull() ?: 1.0
        val montoAhorrado = meta["ahorradoActual"]?.toString()?.toDoubleOrNull() ?: 0.0

        // Llenamos la interfaz
        holder.nombre.text = nombreMeta.uppercase()
        holder.ahorrado.text = String.format(Locale.US, "$ %.2f", montoAhorrado)
        holder.objetivo.text = String.format(Locale.US, "Meta: $ %.2f", montoObjetivo)

        // Calcular progreso para la barra (Máximo 100%)
        val progreso = ((montoAhorrado / montoObjetivo) * 100).toInt()
        holder.barra.progress = if (progreso > 100) 100 else progreso

        // --- CLIC PARA VER EL DETALLE DE ABONOS ---
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, AbonosMetaActivity::class.java)
            intent.putExtra("META_ID", idMeta)
            intent.putExtra("META_NOMBRE", nombreMeta)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = lista.size
}