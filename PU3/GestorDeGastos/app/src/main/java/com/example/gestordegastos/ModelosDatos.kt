package com.example.gestordegastos

import androidx.compose.ui.graphics.Color
import java.util.Date
import java.util.UUID

// Modelos de datos


enum class TemaAplicacion(
    val nombre: String,
    val colorPrimario: Color,
    val colorSecundario: Color,
    val colorFondo: Color
) {
    MORADO(
        nombre = "Morado",
        colorPrimario = Color(0xFF6200EE),
        colorSecundario = Color(0xFF03DAC6),
        colorFondo = Color(0xFFF5F5F5)
    ),
    AZUL(
        nombre = "Azul",
        colorPrimario = Color(0xFF1976D2),
        colorSecundario = Color(0xFF42A5F5),
        colorFondo = Color(0xFFF3F8FF)
    ),
    VERDE(
        nombre = "Verde",
        colorPrimario = Color(0xFF388E3C),
        colorSecundario = Color(0xFF66BB6A),
        colorFondo = Color(0xFFF1F8E9)
    ),
    ROJO(
        nombre = "Rojo",
        colorPrimario = Color(0xFFD32F2F),
        colorSecundario = Color(0xFFEF5350),
        colorFondo = Color(0xFFFFEBEE)
    ),
    NARANJA(
        nombre = "Naranja",
        colorPrimario = Color(0xFFFF9800),
        colorSecundario = Color(0xFFFFB74D),
        colorFondo = Color(0xFFFFF3E0)
    )
}

data class Persona(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val email: String = ""
)

data class ParticipanteGasto(
    val persona: Persona,
    val monto: Double
)

data class Gasto(
    val id: String = UUID.randomUUID().toString(),
    val descripcion: String,
    val monto: Double,
    val pagadoPor: Persona,
    val participantes: List<ParticipanteGasto>, // para incluir montos específicos
    val fecha: Date = Date(),
    val categoria: String = "General",
    val esDivisionIgual: Boolean = true // para saber si es división igual o personalizada
)

data class Deuda(
    val deudor: Persona,
    val acreedor: Persona,
    val monto: Double
)

enum class TipoDivision {
    IGUAL,
    MONTO_ESPECIFICO,
    PORCENTAJE
}

data class PagoDeuda(
    val id: String = UUID.randomUUID().toString(),
    val deudaOriginalId: String,
    val pagador: Persona,
    val receptor: Persona,
    val monto: Double,
    val fecha: Date = Date(),
    val nota: String = "",
    val tipo: TipoPago = TipoPago.MANUAL
)

enum class TipoPago {
    MANUAL
}

data class DeudaConEstado(
    val deuda: Deuda,
    val montoOriginal: Double,
    val montoRestante: Double,
    val pagos: List<PagoDeuda> = emptyList(),
    val estaSaldada: Boolean = false
)