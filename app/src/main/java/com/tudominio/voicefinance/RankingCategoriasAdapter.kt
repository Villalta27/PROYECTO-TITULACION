package com.tudominio.voicefinance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RankingCategoriasAdapter(private val lista: List<CategoriaRanking>) :
    RecyclerView.Adapter<RankingCategoriasAdapter.ViewHolder>() {

    data class CategoriaRanking(val nombre: String, val monto: Double, val color: String)

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val color: View = v.findViewById(R.id.viewColorCategoria)
        val nombre: TextView = v.findViewById(R.id.tvNombreCategoriaItem)
        val monto: TextView = v.findViewById(R.id.tvMontoCategoriaItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_categoria_ranking, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.nombre.text = "${position + 1}. ${item.nombre}"
        holder.monto.text = "$ ${String.format("%.2f", item.monto)}"
        holder.color.setBackgroundColor(Color.parseColor(item.color))
    }

    override fun getItemCount() = lista.size
}