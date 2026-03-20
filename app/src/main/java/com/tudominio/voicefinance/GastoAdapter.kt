package com.tudominio.voicefinance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class GastoAdapter(
    private val lista: List<Map<String, Any>>,
    private val onEditClick: (Map<String, Any>, String) -> Unit
) : RecyclerView.Adapter<GastoAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val titulo: TextView = v.findViewById(R.id.tvTituloGasto)
        val monto: TextView = v.findViewById(R.id.tvMontoGasto)
        val categoria: TextView = v.findViewById(R.id.tvCategoriaGasto)
        val fecha: TextView = v.findViewById(R.id.tvFechaGasto)
        val borde: View = v.findViewById(R.id.viewBordeCategoria)
        val btnMenu: ImageButton = v.findViewById(R.id.btnMenuGasto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_gasto, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        val tipo = item["tipo"]?.toString() ?: "gasto"
        val tituloStr = item["titulo"].toString()
        val montoValue = item["monto"]?.toString()?.toDoubleOrNull() ?: 0.0
        val cat = item["categoria"]?.toString() ?: "Sin Categoría"

        val timestamp = item["fecha"] as? Timestamp
        val fechaFormateada = if (timestamp != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(timestamp.toDate())
        } else { "---" }

        holder.titulo.text = tituloStr.uppercase()
        holder.monto.text = String.format(Locale.US, "$ %.2f", montoValue)
        holder.fecha.text = fechaFormateada

        if (tipo == "ingreso") {
            holder.monto.text = "+ " + holder.monto.text
            holder.monto.setTextColor(Color.parseColor("#00FF88"))
            holder.borde.setBackgroundColor(Color.TRANSPARENT)
            holder.categoria.visibility = View.GONE
        } else {
            holder.monto.text = "- " + holder.monto.text
            holder.monto.setTextColor(Color.parseColor("#FF4C4C"))
            holder.categoria.visibility = View.VISIBLE
            holder.categoria.text = cat

            val colorHex = when (cat) {
                "Comida" -> "#FFB703"
                "Transporte" -> "#219EBC"
                "Vivienda" -> "#8E9AAF"
                "Entretenimiento" -> "#FF006E"
                "Salud" -> "#FB5607"
                "Ahorro" -> "#4361EE"
                else -> "#606C38"
            }
            holder.borde.setBackgroundColor(Color.parseColor(colorHex))
        }

        // --- LÓGICA DE MENÚ CON RESTRICCIÓN DE SEGURIDAD ---
        holder.btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            val esAhorro = tituloStr.contains("Ahorro:", ignoreCase = true)

            // 1. Modificar Nombre: Disponible para TODOS
            popup.menu.add("Modificar Nombre")

            // 2. Modificar Categoría: SOLO para GASTOS (no ahorros, no ingresos)
            if (tipo == "gasto" && !esAhorro) {
                popup.menu.add("Modificar Categoría")
            }

            // 3. Eliminar: SOLO para GASTOS y AHORROS (bloqueado para ingresos)
            if (tipo == "gasto" || esAhorro) {
                popup.menu.add("Eliminar")
            }

            popup.setOnMenuItemClickListener { menuItem ->
                onEditClick(item, menuItem.title.toString())
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = lista.size
}