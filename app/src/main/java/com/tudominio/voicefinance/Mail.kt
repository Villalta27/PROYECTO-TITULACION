package com.tudominio.voicefinance

// Este es el modelo que representa el documento que la extensión de Firebase necesita leer
data class Mail(
    val to: String,
    val message: MailMessage
)

data class MailMessage(
    val subject: String,
    val text: String
)