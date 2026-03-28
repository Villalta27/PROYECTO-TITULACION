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
        // Usamos los IDs del layout simple_list_item_2
        val tvMonto: TextView = v.findViewById(android.R.id.text1)
        val tvDetalle: TextView = v.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val abono = lista[position]

        // Convertimos el monto a Double para poder comparar si es menor a cero
        val montoValue = (abono["monto"] as? Number)?.toDouble() ?: 0.0
        val cartera = abono["desdeCartera"]?.toString() ?: "Desconocido"
        val timestamp = abono["fecha"] as? Timestamp

        val fechaStr = timestamp?.toDate()?.let {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: ""

        // --- LÓGICA DE COLOR Y TEXTO COHERENTE ---
        if (montoValue < 0) {
            // Es un REVERSO: Rojo y signo negativo
            // Usamos Math.abs para que no salga "- $-50", sino "- $ 50"
            holder.tvMonto.text = "- $ ${Math.abs(montoValue)}"
            holder.tvMonto.setTextColor(android.graphics.Color.parseColor("#E63946"))
        } else {
            // Es un ABONO: Verde y signo positivo
            holder.tvMonto.text = "+ $ $montoValue"
            holder.tvMonto.setTextColor(android.graphics.Color.parseColor("#00FF88"))
        }

        holder.tvDetalle.text = "$cartera • $fechaStr"
        holder.tvDetalle.setTextColor(android.graphics.Color.LTGRAY)
    }

    override fun getItemCount() = lista.size
}