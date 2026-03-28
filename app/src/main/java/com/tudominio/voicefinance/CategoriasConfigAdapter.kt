package com.tudominio.voicefinance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoriasConfigAdapter(
    private val lista: List<Map<String, String>>,
    private val onDelete: (String) -> Unit,
    private val onEdit: (String, String) -> Unit
) : RecyclerView.Adapter<CategoriasConfigAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tvNombreCategoriaConfig)
        val btnEdit: ImageView = v.findViewById(R.id.ivEditarCategoria)
        val btnDelete: ImageView = v.findViewById(R.id.ivEliminarCategoria)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria_config, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        val id = item["id"] ?: ""
        val nombreStr = item["nombre"] ?: ""

        holder.nombre.text = nombreStr

        // Configurar clics en los iconos
        holder.btnEdit.setOnClickListener { onEdit(id, nombreStr) }
        holder.btnDelete.setOnClickListener { onDelete(id) }
    }

    override fun getItemCount() = lista.size
}