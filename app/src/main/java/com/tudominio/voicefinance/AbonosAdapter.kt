package com.tudominio.voicefinance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class AbonosAdapter(private val lista: List<Map<String, Any>>) :
    RecyclerView.Adapter<AbonosAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvMonto: TextView = v.findViewById(android.R.id.text1)
        val tvDetalle: TextView = v.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Usamos un diseño simple de Android para ahorrar tiempo, luego puedes personalizarlo
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val abono = lista[position]
        val monto = abono["monto"].toString()
        val cartera = abono["desdeCartera"].toString()
        val timestamp = abono["fecha"] as? Timestamp

        val fechaStr = timestamp?.toDate()?.let {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: ""

        holder.tvMonto.text = "+ $ $monto"
        holder.tvMonto.setTextColor(android.graphics.Color.parseColor("#00FF88"))
        holder.tvDetalle.text = "Desde: $cartera • $fechaStr"
        holder.tvDetalle.setTextColor(android.graphics.Color.LTGRAY)
    }

    override fun getItemCount() = lista.size
}