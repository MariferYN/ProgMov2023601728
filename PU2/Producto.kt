package com.example.productos
import kotlin.io.encoding.Base64

data class Producto(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val descripcion: String,
    val imgBase64: String
)