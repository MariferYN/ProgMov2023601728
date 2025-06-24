package com.example.gestordegastos

import android.content.Context
import android.database.Cursor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SyncManager(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val serverUrl = "http://192.168.100.118:3000/api"

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    data class SyncResult(
        val success: Boolean,
        val message: String,
        val updatedRecords: Int = 0
    )

    suspend fun sincronizarTodos(): SyncResult = withContext(Dispatchers.IO) {
        try {
            Log.e("SYNC_URGENT", "=== BOTÓN SINCRONIZAR PRESIONADO ===")
            Log.e("SYNC_URGENT", "Iniciando sincronización completa...")

            // Sincronizar cada tabla
            Log.d("SyncManager", "Sincronizando personas...")
            val personasResult = sincronizarPersonas()
            Log.d("SyncManager", "Personas result: ${personasResult.message}")
            Log.d("SyncManager", "Sincronizando gastos...")
            val gastosResult = sincronizarGastos()
            Log.d("SyncManager", "Gastos result: ${gastosResult.message}")
            val participantesResult = sincronizarParticipantes()
            val pagosResult = sincronizarPagos()

            val totalUpdated = personasResult.updatedRecords +
                    gastosResult.updatedRecords +
                    participantesResult.updatedRecords +
                    pagosResult.updatedRecords

            if (personasResult.success && gastosResult.success &&
                participantesResult.success && pagosResult.success) {
                SyncResult(true, "Sincronización completada exitosamente", totalUpdated)
            } else {
                SyncResult(false, "Error en la sincronización")
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error en sincronización: ${e.message}", e)
            SyncResult(false, "Error de conexión: ${e.message}")
        }
    }

    private suspend fun sincronizarPersonas(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 1. Obtener datos del servidor
            val serverData = obtenerDatosServidor("personas")

            // 2. Obtener datos locales
            val localData = obtenerDatosLocales("personas")

            // 3. Aplicar sincronización
            val (actualizaciones, inserciones) = compararDatos(localData, serverData)

            // 4. Actualizar base de datos local
            var recordsUpdated = 0
            val db = dbHelper.writableDatabase

            // Insertar/actualizar registros locales
            for (record in actualizaciones + inserciones) {
                val values = android.content.ContentValues().apply {
                    put("id", record.getString("id"))
                    put("nombre", record.getString("nombre"))
                    put("email", record.optString("email"))
                }

                val result = db.insertWithOnConflict(
                    "personas", null, values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
                if (result != -1L) recordsUpdated++
            }

            // 5. Enviar datos locales al servidor
            enviarDatosAlServidor("personas", localData)

            SyncResult(true, "Personas sincronizadas", recordsUpdated)
        } catch (e: Exception) {
            SyncResult(false, "Error sincronizando personas: ${e.message}")
        }
    }

    private suspend fun sincronizarGastos(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val serverData = obtenerDatosServidor("gastos")
            val localData = obtenerDatosLocales("gastos")
            val (actualizaciones, inserciones) = compararDatos(localData, serverData)

            var recordsUpdated = 0
            val db = dbHelper.writableDatabase

            for (record in actualizaciones + inserciones) {
                val values = android.content.ContentValues().apply {
                    put("id", record.getString("id"))
                    put("descripcion", record.getString("descripcion"))
                    put("monto", record.getDouble("monto"))
                    put("pagado_por_id", record.getString("pagado_por_id"))
                    put("fecha", record.getLong("fecha"))
                    put("categoria", record.optString("categoria"))
                    put("division_igual", record.optInt("division_igual"))
                }

                val result = db.insertWithOnConflict(
                    "gastos", null, values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
                if (result != -1L) recordsUpdated++
            }

            enviarDatosAlServidor("gastos", localData)
            SyncResult(true, "Gastos sincronizados", recordsUpdated)
        } catch (e: Exception) {
            SyncResult(false, "Error sincronizando gastos: ${e.message}")
        }
    }

    private suspend fun sincronizarParticipantes(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val serverData = obtenerDatosServidor("participantes")
            val localData = obtenerDatosLocales("participantes")
            val (actualizaciones, inserciones) = compararDatos(localData, serverData)

            var recordsUpdated = 0
            val db = dbHelper.writableDatabase

            for (record in actualizaciones + inserciones) {
                val values = android.content.ContentValues().apply {
                    put("gasto_id", record.getString("gasto_id"))
                    put("persona_id", record.getString("persona_id"))
                    put("monto", record.getDouble("monto"))
                }

                val result = db.insertWithOnConflict(
                    "participantes", null, values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
                if (result != -1L) recordsUpdated++
            }

            enviarDatosAlServidor("participantes", localData)
            SyncResult(true, "Participantes sincronizados", recordsUpdated)
        } catch (e: Exception) {
            SyncResult(false, "Error sincronizando participantes: ${e.message}")
        }
    }

    private suspend fun sincronizarPagos(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val serverData = obtenerDatosServidor("pagos")
            val localData = obtenerDatosLocales("pagos")
            val (actualizaciones, inserciones) = compararDatos(localData, serverData)

            var recordsUpdated = 0
            val db = dbHelper.writableDatabase

            for (record in actualizaciones + inserciones) {
                val values = android.content.ContentValues().apply {
                    put("id", record.getString("id"))
                    put("deuda_original_id", record.optString("deuda_original_id"))
                    put("pagador_id", record.getString("pagador_id"))
                    put("receptor_id", record.getString("receptor_id"))
                    put("monto", record.getDouble("monto"))
                    put("fecha", record.optLong("fecha"))
                    put("nota", record.optString("nota"))
                    put("tipo", record.optString("tipo"))
                }

                val result = db.insertWithOnConflict(
                    "pagos", null, values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
                if (result != -1L) recordsUpdated++
            }

            enviarDatosAlServidor("pagos", localData)
            SyncResult(true, "Pagos sincronizados", recordsUpdated)
        } catch (e: Exception) {
            SyncResult(false, "Error sincronizando pagos: ${e.message}")
        }
    }

    private suspend fun obtenerDatosServidor(tabla: String): JSONArray {
        return try {
            val url = "$serverUrl/$tabla"
            Log.e("HTTP_DEBUG", "=== INICIANDO GET REQUEST ===")
            Log.e("HTTP_DEBUG", "URL: $url")

            val response: HttpResponse = httpClient.get(url)

            Log.e("HTTP_DEBUG", "Respuesta recibida: ${response.status}")
            Log.e("HTTP_DEBUG", "Status code: ${response.status.value}")

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                Log.e("HTTP_DEBUG", "Datos recibidos: $responseText")
                JSONArray(responseText)
            } else {
                Log.e("HTTP_DEBUG", "Error del servidor: ${response.status}")
                throw Exception("Error del servidor: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("HTTP_DEBUG", "ERROR CONECTANDO: ${e.message}", e)
            Log.e("HTTP_DEBUG", "Tipo de excepción: ${e.javaClass.simpleName}")
            throw Exception("Error conectando al servidor: ${e.message}")
        }
    }

    private suspend fun enviarDatosAlServidor(tabla: String, datos: JSONArray) {
        try {
            val url = "$serverUrl/sync/$tabla"
            Log.e("HTTP_DEBUG", "=== INICIANDO POST REQUEST ===")
            Log.e("HTTP_DEBUG", "URL: $url")
            Log.e("HTTP_DEBUG", "Datos a enviar: $datos")

            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(datos.toString())
            }

            Log.e("HTTP_DEBUG", "POST Respuesta: ${response.status}")

            if (!response.status.isSuccess()) {
                Log.e("HTTP_DEBUG", "Error en POST: ${response.status}")
                throw Exception("Error enviando datos al servidor: ${response.status}")
            } else {
                Log.e("HTTP_DEBUG", "POST exitoso")
            }
        } catch (e: Exception) {
            Log.e("HTTP_DEBUG", "ERROR EN POST: ${e.message}", e)
            throw Exception("Error enviando datos: ${e.message}")
        }
    }

    private fun obtenerDatosLocales(tabla: String): JSONArray {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $tabla", null)
        val jsonArray = JSONArray()

        while (cursor.moveToNext()) {
            val jsonObject = JSONObject()
            for (i in 0 until cursor.columnCount) {
                val columnName = cursor.getColumnName(i)
                val value = cursor.getString(i)
                jsonObject.put(columnName, value)
            }
            jsonArray.put(jsonObject)
        }
        cursor.close()
        return jsonArray
    }

    private fun compararDatos(local: JSONArray, servidor: JSONArray): Pair<List<JSONObject>, List<JSONObject>> {
        val actualizaciones = mutableListOf<JSONObject>()
        val inserciones = mutableListOf<JSONObject>()

        // Convertir servidor a lista
        val servidorList = mutableListOf<JSONObject>()
        for (i in 0 until servidor.length()) {
            servidorList.add(servidor.getJSONObject(i))
        }

        // Copiar lista del servidor para inserciones
        inserciones.addAll(servidorList)

        // Comparar datos locales con servidor
        for (i in 0 until local.length()) {
            val localItem = local.getJSONObject(i)
            var encontrado = false

            for (servidorItem in servidorList) {
                val localId = localItem.optString("id", "")
                val servidorId = servidorItem.optString("id", "")

                if (localId == servidorId && localId.isNotEmpty()) {
                    val localFecha = obtenerFechaModificacion(localItem)
                    val servidorFecha = obtenerFechaModificacion(servidorItem)

                    if (localFecha.after(servidorFecha)) {
                        actualizaciones.add(localItem)
                    } else if (servidorFecha.after(localFecha)) {
                        actualizaciones.add(servidorItem)
                    }

                    inserciones.remove(servidorItem)
                    encontrado = true
                    break
                }
            }

            if (!encontrado) {
                inserciones.add(localItem)
            }
        }

        return Pair(actualizaciones, inserciones)
    }

    private fun obtenerFechaModificacion(item: JSONObject): Date {
        val timestamp = item.optLong("fecha_mod_timestamp", 0)
        if (timestamp > 0) {
            return Date(timestamp * 1000) // Convertir de segundos a milisegundos
        }

        val fechaString = item.optString("fecha", "")
        if (fechaString.isNotEmpty()) {
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                return formatter.parse(fechaString) ?: Date(0)
            } catch (e: Exception) {
                val fechaLong = item.optLong("fecha", 0)
                if (fechaLong > 0) {
                    return Date(fechaLong)
                }
            }
        }

        return Date(0)
    }
}