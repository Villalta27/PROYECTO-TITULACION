package com.tudominio.voicefinance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CarteraEstadisticaAdapter(
    private val lista: List<Map<String, String>>,
    private val onClick: (Map<String, String>) -> Unit
) : RecyclerView.Adapter<CarteraEstadisticaAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tvNombreCarteraItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cartera_estadistica, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.nombre.text = item["nombre"]
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = lista.size
}