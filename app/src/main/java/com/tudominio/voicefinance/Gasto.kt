package com.tudominio.voicefinance

data class Gasto(
    val id: String = "",
    val titulo: String = "",
    val monto: Double = 0.0,
    val categoria: String = "",
    val fecha: String = ""
)