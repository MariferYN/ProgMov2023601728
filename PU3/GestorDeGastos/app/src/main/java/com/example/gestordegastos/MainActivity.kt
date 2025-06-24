package com.example.gestordegastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import androidx.lifecycle.ViewModel
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class GastosViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GastosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GastosViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: GastosViewModel
    private lateinit var syncManager: SyncManager // Agregar esta línea

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el sync manager
        syncManager = SyncManager(this) // Agregar esta línea

        val factory = GastosViewModelFactory(applicationContext)
        viewModel = ViewModelProvider(this, factory)[GastosViewModel::class.java]
        setContent {
            GastosCompartidosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AplicacionGastosCompartidos(syncManager = syncManager) // Pasar syncManager
                }
            }
        }
    }
}

class GastosViewModel(context: Context) : ViewModel() {
    private val databaseHelper = DatabaseHelper(context)

    private val _personas = mutableStateListOf<Persona>()
    val personas: List<Persona> = _personas

    private val _gastos = mutableStateListOf<Gasto>()
    val gastos: List<Gasto> = _gastos

    private val _pagosDeudas = mutableStateListOf<PagoDeuda>()

    private val _deudasConEstado = mutableStateListOf<DeudaConEstado>()
    val deudasConEstado: List<DeudaConEstado> = _deudasConEstado

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        cargarPersonas()
        cargarGastos()
        cargarPagos()
        actualizarDeudas()
    }

    // ===== GESTIÓN DE PERSONAS =====

    private fun cargarPersonas() {
        _personas.clear()
        _personas.addAll(databaseHelper.getAllPersonas())
    }

    fun agregarPersona(persona: Persona): String {
        // Validar nombre no vacío
        if (persona.nombre.trim().isEmpty()) {
            return "El nombre no puede estar vacío"
        }

        // Verificar si ya existe
        if (databaseHelper.existePersonaConNombre(persona.nombre)) {
            return "Ya existe una persona con el nombre '${persona.nombre}'"
        }

        val result = databaseHelper.insertPersona(persona)
        if (result != -1L) {
            _personas.add(persona)
            return "success"
        }
        return "Error al guardar en la base de datos"
    }

    fun editarPersona(personaEditada: Persona): String {
        if (personaEditada.nombre.trim().isEmpty()) {
            return "El nombre no puede estar vacío"
        }

        if (databaseHelper.existePersonaConNombre(personaEditada.nombre, personaEditada.id)) {
            return "Ya existe otra persona con el nombre '${personaEditada.nombre}'"
        }

        val result = databaseHelper.updatePersona(personaEditada)
        if (result > 0) {
            val index = _personas.indexOfFirst { it.id == personaEditada.id }
            if (index != -1) {
                _personas[index] = personaEditada
            }
            return "success"
        }
        return "Error al actualizar en la base de datos"
    }

    fun eliminarPersona(persona: Persona): String {

        val deudasPendientes = calcularDeudasConEstadoInterno()
        val tieneDeudas = deudasPendientes.any {
            it.deuda.deudor.id == persona.id || it.deuda.acreedor.id == persona.id
        }
        if (tieneDeudas) {
            return "La persona tiene deudas pendientes"
        }
        val result = databaseHelper.deletePersona(persona.id)
        if (result > 0) {
            _personas.remove(persona)
            refrescarDatos()
            return "success"
        }
        return "Error al eliminar de la base de datos"
    }

    // ===== GESTIÓN DE GASTOS =====

    private fun cargarGastos() {
        _gastos.clear()
        _gastos.addAll(databaseHelper.getAllGastos())
    }

    fun agregarGasto(gasto: Gasto) {
        val result = databaseHelper.insertGasto(gasto, gasto.participantes)
        if (result != -1L) {
            _gastos.add(gasto)
            actualizarDeudas()
        }
    }

    // ===== GESTIÓN DE PAGOS =====

    private fun cargarPagos() {
        _pagosDeudas.clear()
        _pagosDeudas.addAll(databaseHelper.getAllPagos())
        actualizarDeudas()
    }

    private fun registrarPagoDeuda(pago: PagoDeuda) {
        val result = databaseHelper.insertPago(pago)
        if (result != -1L) {
            _pagosDeudas.add(pago)
            actualizarDeudas()
        }
    }

    private fun actualizarDeudas() {
        _deudasConEstado.clear()
        _deudasConEstado.addAll(calcularDeudasConEstadoInterno())
    }

    fun saldarDeudaCompleta(deuda: Deuda, nota: String = "Deuda saldada completamente") {
        val pago = PagoDeuda(
            deudaOriginalId = "${deuda.deudor.id}-${deuda.acreedor.id}",
            pagador = deuda.deudor,
            receptor = deuda.acreedor,
            monto = deuda.monto,
            nota = nota,
            tipo = TipoPago.MANUAL
        )
        registrarPagoDeuda(pago)
    }

    fun registrarPagoParcial(deuda: Deuda, montoPagado: Double, nota: String = "") {
        if (montoPagado > 0 && montoPagado <= deuda.monto) {
            val pago = PagoDeuda(
                deudaOriginalId = "${deuda.deudor.id}-${deuda.acreedor.id}",
                pagador = deuda.deudor,
                receptor = deuda.acreedor,
                monto = montoPagado,
                nota = nota,
                tipo = TipoPago.MANUAL
            )
            registrarPagoDeuda(pago)
        }
    }

    fun obtenerHistorialPagos(): List<PagoDeuda> {
        return _pagosDeudas.sortedByDescending { it.fecha }
    }

    // ===== CÁLCULO DE DEUDAS =====

    private fun calcularDeudas(): List<Deuda> {
        val balances = mutableMapOf<String, Double>()

        // Inicializar balances
        personas.forEach { persona ->
            balances[persona.id] = 0.0
        }

        // Calcular balances por cada gasto
        gastos.forEach { gasto ->
            balances[gasto.pagadoPor.id] = balances[gasto.pagadoPor.id]!! + gasto.monto
            gasto.participantes.forEach { participante ->
                balances[participante.persona.id] = balances[participante.persona.id]!! - participante.monto
            }
        }

        // Generar lista de deudas
        val deudas = mutableListOf<Deuda>()
        val deudores = balances.filter { it.value < -0.01 }.toMutableMap()
        val acreedores = balances.filter { it.value > 0.01 }.toMutableMap()

        for ((deudorId, montoDeuda) in deudores) {
            val deudor = personas.find { it.id == deudorId }!!
            var deudaRestante = -montoDeuda

            for ((acreedorId, montoAcreedor) in acreedores) {
                if (deudaRestante <= 0.01) break
                if (montoAcreedor <= 0.01) continue

                val acreedor = personas.find { it.id == acreedorId }!!
                val montoAPagar = minOf(deudaRestante, montoAcreedor)

                deudas.add(Deuda(deudor, acreedor, montoAPagar))

                deudaRestante -= montoAPagar
                acreedores[acreedorId] = montoAcreedor - montoAPagar
            }
        }

        return deudas.filter { it.monto > 0.01 }
    }

    private fun calcularDeudasConEstadoInterno(): List<DeudaConEstado> {
        val deudasOriginales = calcularDeudas()
        val deudasConEstado = mutableListOf<DeudaConEstado>()

        deudasOriginales.forEach { deuda ->
            val pagosRelacionados = databaseHelper.getPagosByDeudores(deuda.deudor.id, deuda.acreedor.id)

            val totalPagado = pagosRelacionados.sumOf { it.monto }
            val montoRestante = maxOf(0.0, deuda.monto - totalPagado)
            val estaSaldada = montoRestante <= 0.01

            if (!estaSaldada) {
                deudasConEstado.add(
                    DeudaConEstado(
                        deuda = deuda.copy(monto = montoRestante),
                        montoOriginal = deuda.monto,
                        montoRestante = montoRestante,
                        pagos = pagosRelacionados,
                        estaSaldada = false
                    )
                )
            }
        }

        return deudasConEstado
    }

    private fun refrescarDatos() {
        cargarDatos()
    }

    // ===== FUNCIÓN PARA SINCRONIZACIÓN =====

    fun recargarTodosLosDatos() {
        viewModelScope.launch {
            try {
                // Recargar personas
                _personas.clear()
                _personas.addAll(databaseHelper.getAllPersonas())

                // Recargar gastos
                _gastos.clear()
                _gastos.addAll(databaseHelper.getAllGastos())

                // Recargar pagos y actualizar deudas
                _pagosDeudas.clear()
                _pagosDeudas.addAll(databaseHelper.getAllPagos())

                // Actualizar deudas
                actualizarDeudas()

                Log.d("ViewModel", "Datos recargados después de sincronización")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error recargando datos: ${e.message}")
            }
        }
    }

}

@Composable
fun GastosCompartidosTheme(
    tema: TemaAplicacion = TemaAplicacion.MORADO,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = tema.colorPrimario,
            secondary = tema.colorSecundario,
            background = tema.colorFondo
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AplicacionGastosCompartidos(
    syncManager: SyncManager? = null
) {
    val context = LocalContext.current
    val factory = GastosViewModelFactory(context)
    val viewModel: GastosViewModel = viewModel(factory = factory)
    var pantallaActual by remember { mutableStateOf("gastos") }
    var temaActual by remember { mutableStateOf(TemaAplicacion.MORADO) }

    // Estados para la sincronización
    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }

    GastosCompartidosTheme(tema = temaActual) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Gastos Compartidos",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        // Btn sincronización
                        syncManager?.let { manager ->
                            IconButton(
                                onClick = {
                                    Log.e("SYNC_URGENT", "=== BOTÓN SINCRONIZAR PRESIONADO ===")
                                    scope.launch {
                                        isSyncing = true
                                        Log.e("SYNC_URGENT", "Iniciando coroutine de sincronización...")
                                        try {
                                            Log.e("SYNC_URGENT", "Llamando a manager.sincronizarTodos()...")
                                            val resultado = manager.sincronizarTodos()
                                            Log.e("SYNC_URGENT", "Resultado recibido: success=${resultado.success}, message=${resultado.message}")

                                            // Recargar datos después de sincronizar
                                            if (resultado.success) {
                                                viewModel.recargarTodosLosDatos()
                                            }

                                            syncMessage = if (resultado.success) {
                                                "✅ Sincronización exitosa\n${resultado.updatedRecords} registros actualizados"
                                            } else {
                                                "❌ Error: ${resultado.message}"
                                            }
                                        } catch (e: Exception) {
                                            Log.e("SYNC_URGENT", "Excepción capturada: ${e.message}", e)
                                            syncMessage = "❌ Error de conexión: ${e.message}"
                                        } finally {
                                            Log.e("SYNC_URGENT", "Finalizando sincronización...")
                                            isSyncing = false
                                            showSyncDialog = true
                                        }
                                    }
                                }
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = "Sincronizar",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Botón de información
                        IconButton(onClick = { pantallaActual = "informacion" }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Información",
                                tint = Color.White
                            )
                        }

                        // Botón para cambiar tema
                        IconButton(onClick = { pantallaActual = "temas" }) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Cambiar tema",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Dehaze, contentDescription = null) },
                        label = { Text("Gastos") },
                        selected = pantallaActual == "gastos",
                        onClick = { pantallaActual = "gastos" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text("Personas") },
                        selected = pantallaActual == "personas",
                        onClick = { pantallaActual = "personas" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                        label = { Text("Deudas") },
                        selected = pantallaActual == "deudas",
                        onClick = { pantallaActual = "deudas" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text("Historial") },
                        selected = pantallaActual == "historial",
                        onClick = { pantallaActual = "historial" }
                    )
                }
            },
            floatingActionButton = {
                when (pantallaActual) {
                    "gastos" -> {
                        FloatingActionButton(
                            onClick = { pantallaActual = "agregar_gasto" },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar Gasto", tint = Color.White)
                        }
                    }
                    "personas" -> {
                        FloatingActionButton(
                            onClick = { pantallaActual = "agregar_persona" },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Agregar Persona", tint = Color.White)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (pantallaActual) {
                    "gastos" -> PantallaGastos(viewModel)
                    "personas" -> PantallaPersonas(viewModel)
                    "deudas" -> PantallaDeudas(viewModel)
                    "historial" -> PantallaHistorial(viewModel)
                    "agregar_gasto" -> PantallaAgregarGasto(viewModel) { pantallaActual = "gastos" }
                    "agregar_persona" -> PantallaAgregarPersona(viewModel) { pantallaActual = "personas" }
                    "temas" -> PantallaTemas(
                        temaActual = temaActual,
                        onTemaSeleccionado = { temaActual = it },
                        onVolver = { pantallaActual = "gastos" }
                    )
                    "informacion" -> PantallaInformacion()
                }
            }
        }

        // NUEVO: Diálogo para mostrar resultado de sincronización
        if (showSyncDialog) {
            AlertDialog(
                onDismissRequest = { showSyncDialog = false },
                title = { Text("Resultado de Sincronización") },
                text = { Text(syncMessage) },
                confirmButton = {
                    TextButton(onClick = { showSyncDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun PantallaTemas(
    temaActual: TemaAplicacion,
    onTemaSeleccionado: (TemaAplicacion) -> Unit,
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selecciona un tema",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn {
            items(TemaAplicacion.entries) { tema ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onTemaSeleccionado(tema) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (tema == temaActual)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Círculo de color
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    tema.colorPrimario,
                                    shape = CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = tema.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (tema == temaActual) FontWeight.Bold else FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (tema == temaActual) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Seleccionado",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaInformacion() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Encabezado principal
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gastos Compartidos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Controla tus gastos grupales fácilmente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            SeccionInformacion(
                titulo = "¿Cómo funciona?",
                icono = Icons.Default.Help,
                contenido = "Esta aplicación te ayuda a llevar un control de los gastos compartidos entre varias personas. Perfecta para viajes, comidas grupales, gastos de casa compartida o cualquier situación donde necesites dividir gastos."
            )
        }

        item {
            SeccionInformacion(
                titulo = "Agregar Personas",
                icono = Icons.Default.PersonAdd,
                contenido = listOf(
                    "Ve a la pestaña 'Personas'",
                    "Toca el botón '+' para agregar una nueva persona",
                    "Escribe el nombre de la persona",
                    "¡Listo! Ya puedes incluirla en los gastos"
                )
            )
        }

        item {
            SeccionInformacion(
                titulo = "Registrar Gastos",
                icono = Icons.Default.Add,
                contenido = listOf(
                    "Ve a la pestaña 'Gastos'",
                    "Toca el botón '+' para agregar un nuevo gasto",
                    "Describe el gasto y su monto",
                    "Selecciona quién pagó",
                    "Elige entre quién se divide el gasto",
                    "Guarda y el cálculo se hace automáticamente"
                )
            )
        }

        item {
            SeccionInformacion(
                titulo = "Ver Deudas",
                icono = Icons.Default.AccountBalance,
                contenido = "En la pestaña 'Deudas' puedes ver quién le debe a quién y cuánto. La aplicación calcula automáticamente las deudas optimizadas para minimizar el número de transacciones necesarias."
            )
        }

        item {
            SeccionInformacion(
                titulo = "Historial",
                icono = Icons.Default.History,
                contenido = "En el 'Historial' encontrarás un registro completo de todos los gastos realizados, ordenados por fecha. Puedes revisar cualquier gasto anterior."
            )
        }

        item {
            SeccionInformacion(
                titulo = "Personalización",
                icono = Icons.Default.Palette,
                contenido = "Puedes cambiar el tema de colores de la aplicación tocando el icono de paleta en la barra superior. Elige entre varios temas disponibles."
            )
        }

        item {
            // Consejos útiles
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Consejos útiles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val consejos = listOf(
                        "Agrega a todas las personas antes de empezar a registrar gastos",
                        "Usa descripciones claras para identificar fácilmente los gastos",
                        "Revisa regularmente la sección de deudas para mantenerte al día",
                        "El historial te ayuda a recordar gastos anteriores"
                    )

                    consejos.forEach { consejo ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "• ",
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = consejo,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            // Versión de la app
            Text(
                text = "Versión 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SeccionInformacion(
    titulo: String,
    icono: ImageVector,
    contenido: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = contenido,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SeccionInformacion(
    titulo: String,
    icono: ImageVector,
    contenido: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            contenido.forEachIndexed { index, paso ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "${index + 1}. ",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = paso,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PantallaGastos(viewModel: GastosViewModel) {
    val gastos = viewModel.gastos
    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    if (gastos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No hay gastos registrados",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 88.dp // Espacio para el FAB + margen extra
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gastos) { gasto ->
                TarjetaGasto(gasto, formatoFecha)
            }
        }
    }
}

@Composable
fun TarjetaGasto(gasto: Gasto, formatoFecha: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gasto.descripcion,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Pagado por: ${gasto.pagadoPor.nombre}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = formatoFecha.format(gasto.fecha),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = "$${String.format(Locale.getDefault(), "%.2f", gasto.monto)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar división de participantes
            Text(
                text = if (gasto.esDivisionIgual) "División igual:" else "División personalizada:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            gasto.participantes.forEach { participante ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• ${participante.persona.nombre}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%.2f", participante.monto)}",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun PantallaPersonas(viewModel: GastosViewModel) {
    val personas = viewModel.personas
    val context = LocalContext.current

    if (personas.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No hay personas registradas",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(personas) { persona ->
                TarjetaPersona(
                    persona = persona,
                    onEditarPersona = { personaEditada ->
                        val resultado = viewModel.editarPersona(personaEditada)
                        if (resultado != "success") {
                            Toast.makeText(context, resultado, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Persona editada correctamente", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEliminarPersona = { personaAEliminar ->
                        val resultado = viewModel.eliminarPersona(personaAEliminar)
                        if (resultado != "success") {
                            Toast.makeText(context, resultado, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Persona eliminada correctamente", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TarjetaPersona(
    persona: Persona,
    onEditarPersona: (Persona) -> Unit,
    onEliminarPersona: (Persona) -> Unit
) {
    var mostrarMenuOpciones by remember { mutableStateOf(false) }
    var mostrarDialogoEditar by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var nombreEditado by remember(persona) { mutableStateOf(persona.nombre) }
    var emailEditado by remember(persona) { mutableStateOf(persona.email) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = persona.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (persona.email.isNotEmpty()) {
                    Text(
                        text = persona.email,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Box {
                IconButton(onClick = { mostrarMenuOpciones = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = mostrarMenuOpciones,
                    onDismissRequest = { mostrarMenuOpciones = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar")
                            }
                        },
                        onClick = {
                            mostrarMenuOpciones = false
                            mostrarDialogoEditar = true
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Red
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Eliminar", color = Color.Red)
                            }
                        },
                        onClick = {
                            mostrarMenuOpciones = false
                            mostrarDialogoEliminar = true
                        }
                    )
                }
            }
        }
    }

    if (mostrarDialogoEditar) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoEditar = false
                nombreEditado = persona.nombre
                emailEditado = persona.email
            },
            title = {
                Text(
                    "Editar Persona",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreEditado,
                        onValueChange = { nombreEditado = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = emailEditado,
                        onValueChange = { emailEditado = it },
                        label = { Text("Email (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombreEditado.trim().isNotEmpty()) {
                            val personaEditada = persona.copy(
                                nombre = nombreEditado.trim(),
                                email = emailEditado.trim()
                            )
                            onEditarPersona(personaEditada)
                            mostrarDialogoEditar = false
                        }
                    },
                    enabled = nombreEditado.trim().isNotEmpty()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        mostrarDialogoEditar = false
                        nombreEditado = persona.nombre
                        emailEditado = persona.email
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = {
                Text(
                    "Eliminar",
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            },
            text = {
                Column {
                    Text("¿Estás seguro de que quieres eliminar esta persona?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nombre: ${persona.nombre}",
                        fontWeight = FontWeight.Medium
                    )
                    if (persona.email.isNotEmpty()) {
                        Text(
                            text = "Email: ${persona.email}",
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Advertencia: Al eliminar una persona se borrará permanentemente de todos los registros. Solo es posible si no tiene deudas pendientes.",
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Esta acción no se puede deshacer.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEliminarPersona(persona)
                        mostrarDialogoEliminar = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Sí, Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarDialogoEliminar = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PantallaDeudas(viewModel: GastosViewModel) {
    val deudasConEstado = viewModel.deudasConEstado

    if (deudasConEstado.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "¡Todas las cuentas están saldadas!",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(deudasConEstado) { deudaConEstado ->
                TarjetaDeudaConAcciones(deudaConEstado, viewModel)
            }
        }
    }
}

@Composable
fun PantallaAgregarPersona(viewModel: GastosViewModel, onPersonaAgregada: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Agregar Nueva Persona",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (nombre.isNotBlank()) {
                    viewModel.agregarPersona(Persona(nombre = nombre.trim(), email = email.trim()))
                    onPersonaAgregada()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = nombre.isNotBlank()
        ) {
            Text("Agregar Persona")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarGasto(viewModel: GastosViewModel, onGastoAgregado: () -> Unit) {
    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var pagadorSeleccionado by remember { mutableStateOf<Persona?>(null) }
    var mostrarDropdownPagador by remember { mutableStateOf(false) }
    var tipoDivision by remember { mutableStateOf(TipoDivision.IGUAL) }
    var participantesSeleccionados by remember { mutableStateOf(setOf<Persona>()) }
    var montosPersonalizados by remember { mutableStateOf(mapOf<String, String>()) }
    var porcentajesPersonalizados by remember { mutableStateOf(mapOf<String, String>()) }

    val personas = viewModel.personas

    if (personas.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Magenta
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Necesitas agregar personas primero",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Validaciones para habilitar el btn
    val montoNumerico = monto.toDoubleOrNull()
    val esMontoValido = montoNumerico != null && montoNumerico > 0
    val hayParticipantes = participantesSeleccionados.isNotEmpty()
    val esDivisionValida = when (tipoDivision) {
        TipoDivision.IGUAL -> true
        TipoDivision.MONTO_ESPECIFICO -> {
            val totalPersonalizado = participantesSeleccionados.sumOf {
                montosPersonalizados[it.id]?.toDoubleOrNull() ?: 0.0
            }
            abs(totalPersonalizado - (montoNumerico ?: 0.0)) < 0.01
        }
        TipoDivision.PORCENTAJE -> {
            val totalPorcentaje = participantesSeleccionados.sumOf {
                porcentajesPersonalizados[it.id]?.toDoubleOrNull() ?: 0.0
            }
            abs(totalPorcentaje - 100.0) < 0.01
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Agregar Nuevo Gasto",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        item {
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción del gasto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it },
                label = { Text("Monto total") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = mostrarDropdownPagador,
                onExpandedChange = { mostrarDropdownPagador = !mostrarDropdownPagador }
            ) {
                OutlinedTextField(
                    value = pagadorSeleccionado?.nombre ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("¿Quién pagó?") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mostrarDropdownPagador) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = mostrarDropdownPagador,
                    onDismissRequest = { mostrarDropdownPagador = false }
                ) {
                    personas.forEach { persona ->
                        DropdownMenuItem(
                            text = { Text(persona.nombre) },
                            onClick = {
                                pagadorSeleccionado = persona
                                mostrarDropdownPagador = false
                            }
                        )
                    }
                }
            }
        }

        // Selector de tipo de division
        item {
            Text(
                text = "Tipo de división:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { tipoDivision = TipoDivision.IGUAL },
                    label = { Text("Igual") },
                    selected = tipoDivision == TipoDivision.IGUAL,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    onClick = { tipoDivision = TipoDivision.MONTO_ESPECIFICO },
                    label = { Text("Monto") },
                    selected = tipoDivision == TipoDivision.MONTO_ESPECIFICO,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    onClick = { tipoDivision = TipoDivision.PORCENTAJE },
                    label = { Text("%") },
                    selected = tipoDivision == TipoDivision.PORCENTAJE,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Seleccionar participantes:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }

        // Mostrar info de validacion
        if (tipoDivision == TipoDivision.MONTO_ESPECIFICO && montoNumerico != null) {
            item {
                val totalAsignado = participantesSeleccionados.sumOf {
                    montosPersonalizados[it.id]?.toDoubleOrNull() ?: 0.0
                }
                val diferencia = montoNumerico - totalAsignado
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (abs(diferencia) < 0.01) Color.Green.copy(alpha = 0.1f)
                        else Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "Total asignado: $${String.format(Locale.getDefault(), "%.2f", totalAsignado)} / $${String.format(Locale.getDefault(), "%.2f", montoNumerico)}\n" +
                                if (diferencia > 0.01) "Faltan: $${String.format(Locale.getDefault(), "%.2f", diferencia)}"
                                else if (diferencia < -0.01) "Sobran: $${String.format(Locale.getDefault(), "%.2f", -diferencia)}"
                                else "✓ Perfecto",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = if (abs(diferencia) < 0.01) Color.Green.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (tipoDivision == TipoDivision.PORCENTAJE) {
            item {
                val totalPorcentaje = participantesSeleccionados.sumOf {
                    porcentajesPersonalizados[it.id]?.toDoubleOrNull() ?: 0.0
                }
                val diferencia = 100.0 - totalPorcentaje
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (abs(diferencia) < 0.01) Color.Green.copy(alpha = 0.1f)
                        else Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "Total: ${String.format(Locale.getDefault(), "%.1f", totalPorcentaje)}% / 100%\n" +
                                if (diferencia > 0.01) "Faltan: ${String.format(Locale.getDefault(), "%.1f", diferencia)}%"
                                else if (diferencia < -0.01) "Sobran: ${String.format(Locale.getDefault(), "%.1f", -diferencia)}%"
                                else "✓ Perfecto",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = if (abs(diferencia) < 0.01) Color.Green.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.8f)
                    )
                }
            }
        }

        items(personas) { persona ->
            val estaSeleccionado = participantesSeleccionados.contains(persona)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (estaSeleccionado) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = estaSeleccionado,
                            onCheckedChange = { checked ->
                                participantesSeleccionados = if (checked) {
                                    participantesSeleccionados + persona
                                } else {
                                    participantesSeleccionados - persona
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = persona.nombre,
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )

                        // Mostrar monto calculado para division igual
                        if (estaSeleccionado && tipoDivision == TipoDivision.IGUAL && montoNumerico != null) {
                            val montoPorPersona = montoNumerico / participantesSeleccionados.size
                            Text(
                                text = "$${String.format(Locale.getDefault(), "%.2f", montoPorPersona)}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Campos de entrada para division personalizada
                    if (estaSeleccionado && tipoDivision != TipoDivision.IGUAL) {
                        Spacer(modifier = Modifier.height(8.dp))

                        when (tipoDivision) {
                            TipoDivision.MONTO_ESPECIFICO -> {
                                OutlinedTextField(
                                    value = montosPersonalizados[persona.id] ?: "",
                                    onValueChange = { valor ->
                                        montosPersonalizados = montosPersonalizados + (persona.id to valor)
                                    },
                                    label = { Text("Monto para ${persona.nombre}") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("0.00") }
                                )
                            }
                            TipoDivision.PORCENTAJE -> {
                                OutlinedTextField(
                                    value = porcentajesPersonalizados[persona.id] ?: "",
                                    onValueChange = { valor ->
                                        porcentajesPersonalizados = porcentajesPersonalizados + (persona.id to valor)
                                    },
                                    label = { Text("% para ${persona.nombre}") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("0.0") }
                                )

                                // Mostrar monto calculado basado en porcentaje
                                val porcentaje = porcentajesPersonalizados[persona.id]?.toDoubleOrNull()
                                if (porcentaje != null && montoNumerico != null) {
                                    val montoCalculado = (montoNumerico * porcentaje) / 100.0
                                    Text(
                                        text = "Monto: ${String.format(Locale.getDefault(), "%.2f", montoCalculado)}",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Botones para division por porcentajes
        if (tipoDivision == TipoDivision.PORCENTAJE && participantesSeleccionados.isNotEmpty()) {
            item {
                Text(
                    text = "Distribución rápida:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Btn para dividir en partes iguales
                    OutlinedButton(
                        onClick = {
                            val porcentajePorPersona = 100.0 / participantesSeleccionados.size
                            val nuevosPercentajes = participantesSeleccionados.associate { persona ->
                                persona.id to String.format(Locale.getDefault(), "%.1f", porcentajePorPersona)
                            }
                            porcentajesPersonalizados = porcentajesPersonalizados + nuevosPercentajes
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Partes iguales", fontSize = 12.sp)
                    }

                    // Btn para limpiar
                    OutlinedButton(
                        onClick = {
                            porcentajesPersonalizados = porcentajesPersonalizados.filterKeys { key ->
                                !participantesSeleccionados.any { it.id == key }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Limpiar", fontSize = 12.sp)
                    }
                }
            }
        }

        // Botones para division por montos
        if (tipoDivision == TipoDivision.MONTO_ESPECIFICO && participantesSeleccionados.isNotEmpty() && montoNumerico != null) {
            item {
                Text(
                    text = "Distribución rápida:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Btn para dividir en partes iguales
                    OutlinedButton(
                        onClick = {
                            val montoPorPersona = montoNumerico / participantesSeleccionados.size
                            val nuevosMontos = participantesSeleccionados.associate { persona ->
                                persona.id to String.format(Locale.getDefault(), "%.2f", montoPorPersona)
                            }
                            montosPersonalizados = montosPersonalizados + nuevosMontos
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Partes iguales", fontSize = 12.sp)
                    }

                    // Btn para limpiar
                    OutlinedButton(
                        onClick = {
                            montosPersonalizados = montosPersonalizados.filterKeys { key ->
                                !participantesSeleccionados.any { it.id == key }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Limpiar", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (descripcion.isNotBlank() && esMontoValido && pagadorSeleccionado != null &&
                        hayParticipantes && esDivisionValida) {

                        val participantesConMontos = when (tipoDivision) {
                            TipoDivision.IGUAL -> {
                                val montoPorPersona = montoNumerico!! / participantesSeleccionados.size
                                participantesSeleccionados.map { persona ->
                                    ParticipanteGasto(persona, montoPorPersona)
                                }
                            }
                            TipoDivision.MONTO_ESPECIFICO -> {
                                participantesSeleccionados.map { persona ->
                                    val montoPersona = montosPersonalizados[persona.id]?.toDoubleOrNull() ?: 0.0
                                    ParticipanteGasto(persona, montoPersona)
                                }
                            }
                            TipoDivision.PORCENTAJE -> {
                                participantesSeleccionados.map { persona ->
                                    val porcentaje = porcentajesPersonalizados[persona.id]?.toDoubleOrNull() ?: 0.0
                                    val montoPersona = (montoNumerico!! * porcentaje) / 100.0
                                    ParticipanteGasto(persona, montoPersona)
                                }
                            }
                        }

                        val gasto = Gasto(
                            descripcion = descripcion.trim(),
                            monto = montoNumerico!!,
                            pagadoPor = pagadorSeleccionado!!,
                            participantes = participantesConMontos,
                            esDivisionIgual = tipoDivision == TipoDivision.IGUAL
                        )

                        viewModel.agregarGasto(gasto)
                        onGastoAgregado()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = descripcion.isNotBlank() && esMontoValido && pagadorSeleccionado != null &&
                        hayParticipantes && esDivisionValida
            ) {
                Text(
                    text = when {
                        descripcion.isBlank() -> "Ingresa una descripción"
                        !esMontoValido -> "Ingresa un monto válido"
                        pagadorSeleccionado == null -> "Selecciona quién pagó"
                        !hayParticipantes -> "Selecciona participantes"
                        !esDivisionValida -> when (tipoDivision) {
                            TipoDivision.MONTO_ESPECIFICO -> "Los montos no suman el total"
                            TipoDivision.PORCENTAJE -> "Los porcentajes no suman 100%"
                            else -> "Error en la división"
                        }
                        else -> "Agregar Gasto"
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TarjetaDeudaConAcciones(deudaConEstado: DeudaConEstado, viewModel: GastosViewModel) {
    var mostrarDialogoPago by remember { mutableStateOf(false) }
    var mostrarDialogoConfirmacion by remember { mutableStateOf(false) }
    var montoPago by remember { mutableStateOf("") }
    var notaPago by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Info de la deuda
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${deudaConEstado.deuda.deudor.nombre} debe a ${deudaConEstado.deuda.acreedor.nombre}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    if (deudaConEstado.pagos.isNotEmpty()) {
                        Text(
                            text = "Original: $${String.format(Locale.getDefault(), "%.2f", deudaConEstado.montoOriginal)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Text(
                    text = "$${String.format(Locale.getDefault(), "%.2f", deudaConEstado.montoRestante)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Btn saldar completo
                Button(
                    onClick = { mostrarDialogoConfirmacion = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Saldado", fontSize = 12.sp)
                }

                // Btn pago parcial
                OutlinedButton(
                    onClick = { mostrarDialogoPago = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pago Parcial", fontSize = 12.sp)
                }
            }

            // Mostrar pagos anteriores si existen
            if (deudaConEstado.pagos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pagos realizados:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                deudaConEstado.pagos.takeLast(2).forEach { pago ->
                    Text(
                        text = "• $${String.format(Locale.getDefault(), "%.2f", pago.monto)} - ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(pago.fecha)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    //Saldar deuda
    if (mostrarDialogoConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmacion = false },
            title = {
                Text(
                    "Confirmar Saldo Completo",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("¿Estás seguro de que quieres saldar completamente esta deuda?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Deudor: ${deudaConEstado.deuda.deudor.nombre}",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Acreedor: ${deudaConEstado.deuda.acreedor.nombre}",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Monto a saldar: $${String.format(Locale.getDefault(), "%.2f", deudaConEstado.montoRestante)}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esta acción no se puede deshacer.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saldarDeudaCompleta(
                            deudaConEstado.deuda,
                            "Deuda saldada completamente"
                        )
                        mostrarDialogoConfirmacion = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Sí, Saldar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarDialogoConfirmacion = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Pago parcial
    if (mostrarDialogoPago) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPago = false },
            title = { Text("Registrar Pago Parcial") },
            text = {
                Column {
                    Text("Deuda actual: $${String.format(Locale.getDefault(), "%.2f", deudaConEstado.montoRestante)}")
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = montoPago,
                        onValueChange = { montoPago = it },
                        label = { Text("Monto pagado") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notaPago,
                        onValueChange = { notaPago = it },
                        label = { Text("Nota (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val monto = montoPago.toDoubleOrNull()
                        if (monto != null && monto > 0 && monto <= deudaConEstado.montoRestante) {
                            viewModel.registrarPagoParcial(deudaConEstado.deuda, monto, notaPago)
                            mostrarDialogoPago = false
                            montoPago = ""
                            notaPago = ""
                        }
                    }
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPago = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PantallaHistorial(viewModel: GastosViewModel) {
    val pagos = viewModel.obtenerHistorialPagos()
    val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    if (pagos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No hay pagos registrados",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pagos) { pago ->
                TarjetaPago(pago, formatoFecha)
            }
        }
    }
}

@Composable
fun TarjetaPago(pago: PagoDeuda, formatoFecha: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${pago.pagador.nombre} pagó a ${pago.receptor.nombre}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = formatoFecha.format(pago.fecha),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (pago.nota.isNotEmpty()) {
                        Text(
                            text = pago.nota,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Text(
                    text = "$${String.format(Locale.getDefault(), "%.2f", pago.monto)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}
