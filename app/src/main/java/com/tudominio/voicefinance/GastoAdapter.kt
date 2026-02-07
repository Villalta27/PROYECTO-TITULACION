package com.tudominio.voicefinance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GastoAdapter(private val lista: List<Map<String, Any>>) :
    RecyclerView.Adapter<GastoAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val titulo: TextView = v.findViewById(R.id.tvTituloGasto)
        val monto: TextView = v.findViewById(R.id.tvMontoGasto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_gasto, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        val tipo = item["tipo"]?.toString() ?: "gasto"
        val montoValue = item["monto"]?.toString() ?: "0.0"

        holder.titulo.text = item["titulo"].toString()

        if (tipo == "ingreso") {
            holder.monto.text = "+ $ $montoValue"
            holder.monto.setTextColor(Color.parseColor("#00FF88")) // Verde
        } else {
            holder.monto.text = "- $ $montoValue"
            holder.monto.setTextColor(Color.parseColor("#FF4C4C")) // Rojo
        }
    }

    override fun getItemCount() = lista.size
}