package com.tudominio.voicefinance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FinanzaAdapter(
    private var lista: List<Map<String, Any>>,
    private val esModoLectura: Boolean = false, // <--- PASO 1: Agregamos este switch (falso por defecto)
    private val onItemClick: (String, String) -> Unit,
    private val onOpcionesClick: (String, String, View) -> Unit
) : RecyclerView.Adapter<FinanzaAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tvNombreItem)
        val balance: TextView = v.findViewById(R.id.tvBalanceItem)
        val fechas: TextView = v.findViewById(R.id.tvFechasItem)
        val btnOpciones: ImageButton = v.findViewById(R.id.btnOpcionesFinanza)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_finanza, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        val id = item["id"]?.toString() ?: ""
        val nombre = item["nombre"]?.toString() ?: "Sin nombre"
        val balance = item["balance"]?.toString() ?: "0.00"
        val creado = item["fechaCreacion"]?.toString() ?: "--/--/--"

        holder.nombre.text = nombre
        holder.balance.text = "Balance: $ $balance"
        holder.fechas.text = "Creado: $creado"

        // --- PASO 2: LÓGICA DE VISIBILIDAD ---
        if (esModoLectura) {
            // Si es historial, escondemos el botón de opciones por completo
            holder.btnOpciones.visibility = View.GONE
        } else {
            // Si es la Home, lo mostramos y activamos su clic
            holder.btnOpciones.visibility = View.VISIBLE
            holder.btnOpciones.setOnClickListener {
                if (id.isNotEmpty()) {
                    onOpcionesClick(id, nombre, it)
                }
            }
        }

        holder.itemView.setOnClickListener {
            if (id.isNotEmpty()) {
                onItemClick(id, nombre)
            }
        }
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<Map<String, Any>>) {
        this.lista = nuevaLista
        notifyDataSetChanged()
    }
}