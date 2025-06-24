package com.example.gestordegastos

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "gastos.db"
        private const val DATABASE_VERSION = 2

        // Tabla Personas
        private const val TABLE_PERSONAS = "personas"
        private const val COLUMN_PERSONA_ID = "id"
        private const val COLUMN_PERSONA_NOMBRE = "nombre"
        private const val COLUMN_PERSONA_EMAIL = "email"

        // Tabla Gastos
        private const val TABLE_GASTOS = "gastos"
        private const val COLUMN_GASTO_ID = "id"
        private const val COLUMN_GASTO_DESCRIPCION = "descripcion"
        private const val COLUMN_GASTO_MONTO = "monto"
        private const val COLUMN_GASTO_PAGADO_POR = "pagado_por_id"
        private const val COLUMN_GASTO_FECHA = "fecha"
        private const val COLUMN_GASTO_CATEGORIA = "categoria"
        private const val COLUMN_GASTO_DIVISION_IGUAL = "division_igual"

        // Tabla Participantes
        private const val TABLE_PARTICIPANTES = "participantes"
        private const val COLUMN_PARTICIPANTE_GASTO_ID = "gasto_id"
        private const val COLUMN_PARTICIPANTE_PERSONA_ID = "persona_id"
        private const val COLUMN_PARTICIPANTE_MONTO = "monto"

        // Tabla Pagos
        private const val TABLE_PAGOS = "pagos"
        private const val COLUMN_PAGO_ID = "id"
        private const val COLUMN_PAGO_DEUDA_ID = "deuda_original_id"
        private const val COLUMN_PAGO_PAGADOR_ID = "pagador_id"
        private const val COLUMN_PAGO_RECEPTOR_ID = "receptor_id"
        private const val COLUMN_PAGO_MONTO = "monto"
        private const val COLUMN_PAGO_FECHA = "fecha"
        private const val COLUMN_PAGO_NOTA = "nota"
        private const val COLUMN_PAGO_TIPO = "tipo"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla Personas
        val createPersonasTable = """
            CREATE TABLE $TABLE_PERSONAS (
                $COLUMN_PERSONA_ID TEXT PRIMARY KEY,
                $COLUMN_PERSONA_NOMBRE TEXT NOT NULL,
                $COLUMN_PERSONA_EMAIL TEXT,
                fecha_mod INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()

        // Crear tabla Gastos
        val createGastosTable = """
            CREATE TABLE $TABLE_GASTOS (
                $COLUMN_GASTO_ID TEXT PRIMARY KEY,
                $COLUMN_GASTO_DESCRIPCION TEXT NOT NULL,
                $COLUMN_GASTO_MONTO REAL NOT NULL,
                $COLUMN_GASTO_PAGADO_POR TEXT NOT NULL,
                $COLUMN_GASTO_FECHA INTEGER NOT NULL,
                $COLUMN_GASTO_CATEGORIA TEXT,
                $COLUMN_GASTO_DIVISION_IGUAL INTEGER,
                fecha_mod INTEGER DEFAULT (strftime('%s', 'now')),
                FOREIGN KEY($COLUMN_GASTO_PAGADO_POR) REFERENCES $TABLE_PERSONAS($COLUMN_PERSONA_ID)
            )
        """.trimIndent()

        // Crear tabla Participantes
        val createParticipantesTable = """
            CREATE TABLE $TABLE_PARTICIPANTES (
                $COLUMN_PARTICIPANTE_GASTO_ID TEXT,
                $COLUMN_PARTICIPANTE_PERSONA_ID TEXT,
                $COLUMN_PARTICIPANTE_MONTO REAL,
                fecha_mod INTEGER DEFAULT (strftime('%s', 'now')),
                PRIMARY KEY($COLUMN_PARTICIPANTE_GASTO_ID, $COLUMN_PARTICIPANTE_PERSONA_ID),
                FOREIGN KEY($COLUMN_PARTICIPANTE_GASTO_ID) REFERENCES $TABLE_GASTOS($COLUMN_GASTO_ID),
                FOREIGN KEY($COLUMN_PARTICIPANTE_PERSONA_ID) REFERENCES $TABLE_PERSONAS($COLUMN_PERSONA_ID)
            )
        """.trimIndent()

        // Crear tabla Pagos
        val createPagosTable = """
            CREATE TABLE $TABLE_PAGOS (
                $COLUMN_PAGO_ID TEXT PRIMARY KEY,
                $COLUMN_PAGO_DEUDA_ID TEXT,
                $COLUMN_PAGO_PAGADOR_ID TEXT,
                $COLUMN_PAGO_RECEPTOR_ID TEXT,
                $COLUMN_PAGO_MONTO REAL,
                $COLUMN_PAGO_FECHA INTEGER,
                $COLUMN_PAGO_NOTA TEXT,
                $COLUMN_PAGO_TIPO TEXT,
                fecha_mod INTEGER DEFAULT (strftime('%s', 'now')),
                FOREIGN KEY($COLUMN_PAGO_PAGADOR_ID) REFERENCES $TABLE_PERSONAS($COLUMN_PERSONA_ID),
                FOREIGN KEY($COLUMN_PAGO_RECEPTOR_ID) REFERENCES $TABLE_PERSONAS($COLUMN_PERSONA_ID)
            )
        """.trimIndent()

        // Crear triggers para actualizar fecha_mod automáticamente
        val updatePersonasTrigger = """
            CREATE TRIGGER update_personas_fecha_mod 
            AFTER UPDATE ON $TABLE_PERSONAS
            BEGIN
                UPDATE $TABLE_PERSONAS SET fecha_mod = strftime('%s', 'now') WHERE id = NEW.id;
            END
        """.trimIndent()

        val updateGastosTrigger = """
            CREATE TRIGGER update_gastos_fecha_mod 
            AFTER UPDATE ON $TABLE_GASTOS
            BEGIN
                UPDATE $TABLE_GASTOS SET fecha_mod = strftime('%s', 'now') WHERE id = NEW.id;
            END
        """.trimIndent()

        val updateParticipantesTrigger = """
            CREATE TRIGGER update_participantes_fecha_mod 
            AFTER UPDATE ON $TABLE_PARTICIPANTES
            BEGIN
                UPDATE $TABLE_PARTICIPANTES SET fecha_mod = strftime('%s', 'now') 
                WHERE gasto_id = NEW.gasto_id AND persona_id = NEW.persona_id;
            END
        """.trimIndent()

        val updatePagosTrigger = """
            CREATE TRIGGER update_pagos_fecha_mod 
            AFTER UPDATE ON $TABLE_PAGOS
            BEGIN
                UPDATE $TABLE_PAGOS SET fecha_mod = strftime('%s', 'now') WHERE id = NEW.id;
            END
        """.trimIndent()

        db.execSQL(createPersonasTable)
        db.execSQL(createGastosTable)
        db.execSQL(createParticipantesTable)
        db.execSQL(createPagosTable)

        db.execSQL(updatePersonasTrigger)
        db.execSQL(updateGastosTrigger)
        db.execSQL(updateParticipantesTrigger)
        db.execSQL(updatePagosTrigger)


    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when (oldVersion) {
            1 -> {
                try {
                    db.execSQL("ALTER TABLE $TABLE_PERSONAS ADD COLUMN fecha_mod INTEGER DEFAULT (strftime('%s', 'now'))")
                    db.execSQL("ALTER TABLE $TABLE_GASTOS ADD COLUMN fecha_mod INTEGER DEFAULT (strftime('%s', 'now'))")
                    db.execSQL("ALTER TABLE $TABLE_PARTICIPANTES ADD COLUMN fecha_mod INTEGER DEFAULT (strftime('%s', 'now'))")
                    db.execSQL("ALTER TABLE $TABLE_PAGOS ADD COLUMN fecha_mod INTEGER DEFAULT (strftime('%s', 'now'))")

                    // Triggers para actualizacion automatica
                    val updatePersonasTrigger = """
                    CREATE TRIGGER update_personas_fecha_mod 
                    AFTER UPDATE ON $TABLE_PERSONAS
                    BEGIN
                        UPDATE $TABLE_PERSONAS SET fecha_mod = strftime('%s', 'now') WHERE id = NEW.id;
                    END
                """.trimIndent()

                    val updateGastosTrigger = """
                    CREATE TRIGGER update_gastos_fecha_mod 
                    AFTER UPDATE ON $TABLE_GASTOS
                    BEGIN
                        UPDATE $TABLE_GASTOS SET fecha_mod = strftime('%s', 'now') WHERE id = NEW.id;
                    END
                """.trimIndent()

                    val updateParticipantesTrigger = """
                    CREATE TRIGGER update_participantes_fecha_mod 
                    AFTER UPDATE ON $TABLE_PARTICIPANTES
                    BEGIN
                        UPDATE $TABLE_PARTICIPANTES SET fecha_mod = strftime('%s', 'now') 
                        WHERE gasto_id = NEW.gasto_id AND persona_id = NEW.persona_id;
                    END
                """.trimIndent()

                    val updatePagosTrigger = """
                    CREATE TRIGGER update_pagos_fecha_mod 
                    AFTER UPDATE ON $TABLE_PAGOS
                    BEGIN
                        UPDATE $TABLE_PAGOS SET fecha_mod = strftime('%s', 'now') WHERE id = NEW.id;
                    END
                """.trimIndent()

                    db.execSQL(updatePersonasTrigger)
                    db.execSQL(updateGastosTrigger)
                    db.execSQL(updateParticipantesTrigger)
                    db.execSQL(updatePagosTrigger)

                } catch (e: Exception) {
                    db.execSQL("DROP TABLE IF EXISTS $TABLE_PAGOS")
                    db.execSQL("DROP TABLE IF EXISTS $TABLE_PARTICIPANTES")
                    db.execSQL("DROP TABLE IF EXISTS $TABLE_GASTOS")
                    db.execSQL("DROP TABLE IF EXISTS $TABLE_PERSONAS")
                    onCreate(db)
                }
            }
            else -> {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_PAGOS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_PARTICIPANTES")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_GASTOS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_PERSONAS")
                onCreate(db)
            }
        }
    }

    // ===== CRUD PERSONAS =====

    fun insertPersona(persona: Persona): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PERSONA_ID, persona.id)
            put(COLUMN_PERSONA_NOMBRE, persona.nombre)
            put(COLUMN_PERSONA_EMAIL, persona.email)
        }
        return db.insert(TABLE_PERSONAS, null, values)
    }

    fun getAllPersonas(): List<Persona> {
        val personas = mutableListOf<Persona>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PERSONAS, null, null, null, null, null, COLUMN_PERSONA_NOMBRE)

        cursor.use {
            while (it.moveToNext()) {
                personas.add(
                    Persona(
                        id = it.getString(it.getColumnIndexOrThrow(COLUMN_PERSONA_ID)),
                        nombre = it.getString(it.getColumnIndexOrThrow(COLUMN_PERSONA_NOMBRE)),
                        email = it.getString(it.getColumnIndexOrThrow(COLUMN_PERSONA_EMAIL)) ?: ""
                    )
                )
            }
        }
        return personas
    }

    fun updatePersona(persona: Persona): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PERSONA_NOMBRE, persona.nombre)
            put(COLUMN_PERSONA_EMAIL, persona.email)
        }
        return db.update(TABLE_PERSONAS, values, "$COLUMN_PERSONA_ID = ?", arrayOf(persona.id))
    }

    fun deletePersona(personaId: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_PERSONAS, "$COLUMN_PERSONA_ID = ?", arrayOf(personaId))
    }

    fun existePersonaConNombre(nombre: String, excludeId: String? = null): Boolean {
        val db = readableDatabase
        val whereClause = if (excludeId != null) {
            "$COLUMN_PERSONA_NOMBRE = ? AND $COLUMN_PERSONA_ID != ?"
        } else {
            "$COLUMN_PERSONA_NOMBRE = ?"
        }

        val whereArgs = if (excludeId != null) {
            arrayOf(nombre.trim(), excludeId)
        } else {
            arrayOf(nombre.trim())
        }

        val cursor = db.query(
            TABLE_PERSONAS,
            arrayOf("COUNT(*)"),
            whereClause,
            whereArgs,
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0) > 0
            }
        }
        return false
    }
    // ===== CRUD GASTOS =====

    fun insertGasto(gasto: Gasto, participantes: List<ParticipanteGasto>): Long {
        val db = writableDatabase
        db.beginTransaction()

        try {
            // Insertar gasto
            val gastoValues = ContentValues().apply {
                put(COLUMN_GASTO_ID, gasto.id)
                put(COLUMN_GASTO_DESCRIPCION, gasto.descripcion)
                put(COLUMN_GASTO_MONTO, gasto.monto)
                put(COLUMN_GASTO_PAGADO_POR, gasto.pagadoPor.id)
                put(COLUMN_GASTO_FECHA, gasto.fecha.time)
                put(COLUMN_GASTO_CATEGORIA, gasto.categoria)
                put(COLUMN_GASTO_DIVISION_IGUAL, if (gasto.esDivisionIgual) 1 else 0)
            }
            val gastoResult = db.insert(TABLE_GASTOS, null, gastoValues)

            // Insertar participantes
            participantes.forEach { participante ->
                val participanteValues = ContentValues().apply {
                    put(COLUMN_PARTICIPANTE_GASTO_ID, gasto.id)
                    put(COLUMN_PARTICIPANTE_PERSONA_ID, participante.persona.id)
                    put(COLUMN_PARTICIPANTE_MONTO, participante.monto)
                }
                db.insert(TABLE_PARTICIPANTES, null, participanteValues)
            }

            db.setTransactionSuccessful()
            return gastoResult
        } finally {
            db.endTransaction()
        }
    }

    fun getAllGastos(): List<Gasto> {
        val gastos = mutableListOf<Gasto>()
        val db = readableDatabase

        val query = """
            SELECT g.*, p.nombre as pagador_nombre, p.email as pagador_email
            FROM $TABLE_GASTOS g
            INNER JOIN $TABLE_PERSONAS p ON g.$COLUMN_GASTO_PAGADO_POR = p.$COLUMN_PERSONA_ID
            ORDER BY g.$COLUMN_GASTO_FECHA DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        cursor.use {
            while (it.moveToNext()) {
                val gastoId = it.getString(it.getColumnIndexOrThrow(COLUMN_GASTO_ID))
                val pagador = Persona(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_GASTO_PAGADO_POR)),
                    nombre = it.getString(it.getColumnIndexOrThrow("pagador_nombre")),
                    email = it.getString(it.getColumnIndexOrThrow("pagador_email")) ?: ""
                )

                val participantes = getParticipantesByGastoId(gastoId)

                gastos.add(
                    Gasto(
                        id = gastoId,
                        descripcion = it.getString(it.getColumnIndexOrThrow(COLUMN_GASTO_DESCRIPCION)),
                        monto = it.getDouble(it.getColumnIndexOrThrow(COLUMN_GASTO_MONTO)),
                        pagadoPor = pagador,
                        participantes = participantes,
                        fecha = java.util.Date(it.getLong(it.getColumnIndexOrThrow(COLUMN_GASTO_FECHA))),
                        categoria = it.getString(it.getColumnIndexOrThrow(COLUMN_GASTO_CATEGORIA)) ?: "General",
                        esDivisionIgual = it.getInt(it.getColumnIndexOrThrow(COLUMN_GASTO_DIVISION_IGUAL)) == 1
                    )
                )
            }
        }
        return gastos
    }

    private fun getParticipantesByGastoId(gastoId: String): List<ParticipanteGasto> {
        val participantes = mutableListOf<ParticipanteGasto>()
        val db = readableDatabase

        val query = """
            SELECT part.*, p.nombre, p.email
            FROM $TABLE_PARTICIPANTES part
            INNER JOIN $TABLE_PERSONAS p ON part.$COLUMN_PARTICIPANTE_PERSONA_ID = p.$COLUMN_PERSONA_ID
            WHERE part.$COLUMN_PARTICIPANTE_GASTO_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(gastoId))

        cursor.use {
            while (it.moveToNext()) {
                val persona = Persona(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_PARTICIPANTE_PERSONA_ID)),
                    nombre = it.getString(it.getColumnIndexOrThrow("nombre")),
                    email = it.getString(it.getColumnIndexOrThrow("email")) ?: ""
                )

                participantes.add(
                    ParticipanteGasto(
                        persona = persona,
                        monto = it.getDouble(it.getColumnIndexOrThrow(COLUMN_PARTICIPANTE_MONTO))
                    )
                )
            }
        }
        return participantes
    }

    // ===== CRUD PAGOS =====

    fun insertPago(pago: PagoDeuda): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PAGO_ID, pago.id)
            put(COLUMN_PAGO_DEUDA_ID, pago.deudaOriginalId)
            put(COLUMN_PAGO_PAGADOR_ID, pago.pagador.id)
            put(COLUMN_PAGO_RECEPTOR_ID, pago.receptor.id)
            put(COLUMN_PAGO_MONTO, pago.monto)
            put(COLUMN_PAGO_FECHA, pago.fecha.time)
            put(COLUMN_PAGO_NOTA, pago.nota)
            put(COLUMN_PAGO_TIPO, pago.tipo.name)
        }
        return db.insert(TABLE_PAGOS, null, values)
    }

    fun getAllPagos(): List<PagoDeuda> {
        val pagos = mutableListOf<PagoDeuda>()
        val db = readableDatabase

        val query = """
            SELECT pg.*, 
                   p1.nombre as pagador_nombre, p1.email as pagador_email,
                   p2.nombre as receptor_nombre, p2.email as receptor_email
            FROM $TABLE_PAGOS pg
            INNER JOIN $TABLE_PERSONAS p1 ON pg.$COLUMN_PAGO_PAGADOR_ID = p1.$COLUMN_PERSONA_ID
            INNER JOIN $TABLE_PERSONAS p2 ON pg.$COLUMN_PAGO_RECEPTOR_ID = p2.$COLUMN_PERSONA_ID
            ORDER BY pg.$COLUMN_PAGO_FECHA DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        cursor.use {
            while (it.moveToNext()) {
                val pagador = Persona(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_PAGADOR_ID)),
                    nombre = it.getString(it.getColumnIndexOrThrow("pagador_nombre")),
                    email = it.getString(it.getColumnIndexOrThrow("pagador_email")) ?: ""
                )

                val receptor = Persona(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_RECEPTOR_ID)),
                    nombre = it.getString(it.getColumnIndexOrThrow("receptor_nombre")),
                    email = it.getString(it.getColumnIndexOrThrow("receptor_email")) ?: ""
                )

                pagos.add(
                    PagoDeuda(
                        id = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_ID)),
                        deudaOriginalId = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_DEUDA_ID)),
                        pagador = pagador,
                        receptor = receptor,
                        monto = it.getDouble(it.getColumnIndexOrThrow(COLUMN_PAGO_MONTO)),
                        fecha = java.util.Date(it.getLong(it.getColumnIndexOrThrow(COLUMN_PAGO_FECHA))),
                        nota = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_NOTA)) ?: "",
                        tipo = TipoPago.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_TIPO)))
                    )
                )
            }
        }
        return pagos
    }

    fun getPagosByDeudores(pagadorId: String, receptorId: String): List<PagoDeuda> {
        val pagos = mutableListOf<PagoDeuda>()
        val db = readableDatabase

        val query = """
            SELECT pg.*, 
                   p1.nombre as pagador_nombre, p1.email as pagador_email,
                   p2.nombre as receptor_nombre, p2.email as receptor_email
            FROM $TABLE_PAGOS pg
            INNER JOIN $TABLE_PERSONAS p1 ON pg.$COLUMN_PAGO_PAGADOR_ID = p1.$COLUMN_PERSONA_ID
            INNER JOIN $TABLE_PERSONAS p2 ON pg.$COLUMN_PAGO_RECEPTOR_ID = p2.$COLUMN_PERSONA_ID
            WHERE pg.$COLUMN_PAGO_PAGADOR_ID = ? AND pg.$COLUMN_PAGO_RECEPTOR_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(pagadorId, receptorId))

        cursor.use {
            while (it.moveToNext()) {
                val pagador = Persona(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_PAGADOR_ID)),
                    nombre = it.getString(it.getColumnIndexOrThrow("pagador_nombre")),
                    email = it.getString(it.getColumnIndexOrThrow("pagador_email")) ?: ""
                )

                val receptor = Persona(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_RECEPTOR_ID)),
                    nombre = it.getString(it.getColumnIndexOrThrow("receptor_nombre")),
                    email = it.getString(it.getColumnIndexOrThrow("receptor_email")) ?: ""
                )

                pagos.add(
                    PagoDeuda(
                        id = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_ID)),
                        deudaOriginalId = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_DEUDA_ID)),
                        pagador = pagador,
                        receptor = receptor,
                        monto = it.getDouble(it.getColumnIndexOrThrow(COLUMN_PAGO_MONTO)),
                        fecha = java.util.Date(it.getLong(it.getColumnIndexOrThrow(COLUMN_PAGO_FECHA))),
                        nota = it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_NOTA)) ?: "",
                        tipo = TipoPago.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_PAGO_TIPO)))
                    )
                )
            }
        }
        return pagos
    }
}