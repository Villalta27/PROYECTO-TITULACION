package com.tudominio.voicefinance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class GastoAdapter(private val listaGastos: List<Map<String, Any>>) :
    RecyclerView.Adapter<GastoAdapter.GastoViewHolder>() {

    class GastoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.tvItemTitulo)
        val fecha: TextView = view.findViewById(R.id.tvItemFecha)
        val monto: TextView = view.findViewById(R.id.tvItemMonto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gasto, parent, false)
        return GastoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        val gasto = listaGastos[position]
        holder.titulo.text = gasto["titulo"].toString().uppercase()

        // Formatear el monto a 2 decimales
        val montoValue = gasto["monto"]?.toString()?.toDouble() ?: 0.0
        holder.monto.text = "$ ${String.format("%.2f", montoValue)}"

        // Formatear fecha y hora de registro
        val timestamp = gasto["fecha"] as? com.google.firebase.Timestamp
        if (timestamp != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.fecha.text = sdf.format(timestamp.toDate())
        }
    }

    override fun getItemCount() = listaGastos.size
}