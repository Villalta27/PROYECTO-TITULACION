package com.tudominio.voicefinance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FinanzaAdapter(
    private var lista: List<Map<String, Any>>,
    private val onItemClick: (String, String) -> Unit, // Para abrir el Historial o el Home
    private val onOpcionesClick: (String, String, View) -> Unit // Para el menú de tres puntos
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

        // Extraemos los datos del Map de forma segura
        val id = item["id"]?.toString() ?: ""
        val nombre = item["nombre"]?.toString() ?: "Sin nombre"
        val balance = item["balance"]?.toString() ?: "0.00"
        val creado = item["fechaCreacion"]?.toString() ?: "--/--/--"

        holder.nombre.text = nombre
        holder.balance.text = "Balance: $ $balance"
        holder.fechas.text = "Creado: $creado"

        // EVENTO 1: Al tocar cualquier parte de la tarjeta, ejecutamos onItemClick
        holder.itemView.setOnClickListener {
            if (id.isNotEmpty()) {
                onItemClick(id, nombre)
            }
        }

        // EVENTO 2: Al tocar el botón de opciones (tres puntos)
        holder.btnOpciones.setOnClickListener {
            if (id.isNotEmpty()) {
                onOpcionesClick(id, nombre, it)
            }
        }
    }

    override fun getItemCount() = lista.size

    // Función para refrescar la lista desde Firebase
    fun actualizarLista(nuevaLista: List<Map<String, Any>>) {
        this.lista = nuevaLista
        notifyDataSetChanged()
    }
}