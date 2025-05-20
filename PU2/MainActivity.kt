package com.example.productos

import androidx.compose.ui.window.Dialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import com.example.productos.ui.theme.Typography
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.productos.ui.theme.ProductosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Icon
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = this
            val preferences = remember { ThemePreferences(context) }

            val darkTheme by preferences.darkThemeFlow.collectAsState(initial = false)
            val colorPrimaryInt by preferences.colorFlow.collectAsState(initial = 0xFF6200EE.toInt())

            val primaryColor = Color(colorPrimaryInt)

            ProductosTheme(
                darkTheme = darkTheme,
                primaryColor = primaryColor
            ) {
                AppProductos(preferences)
            }
        }


    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(navController: NavController, preferences: ThemePreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ✅ Leemos el tema y color directamente desde DataStore (reactivo)
    val darkTheme by preferences.darkThemeFlow.collectAsState(initial = false)
    val selectedColor by preferences.colorFlow
        .map { Color(it) }
        .collectAsState(initial = Color(0xFF6200EE))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navController.currentBackStackEntry?.destination?.route != "lista") {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Modo de Apariencia", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Claro")
                Switch(
                    checked = darkTheme,
                    onCheckedChange = {
                        scope.launch {
                            preferences.saveDarkTheme(it)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text("Oscuro")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Color Primario", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val colores = listOf(Color(0xFFD25931), Color(0xFF009688), Color(0xFF9A87E5))
                colores.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selectedColor == color) 4.dp else 2.dp,
                                color = if (selectedColor == color) Color.Black else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                scope.launch {
                                    preferences.saveColor(color.toArgb())
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Puedes cambiar el modo y color del tema. La configuración se guarda automáticamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAyuda(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca del sistema") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navController.currentBackStackEntry?.destination?.route != "lista") {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())  // ← Aquí está la clave
        ) {
            Text(
                "¿Para qué sirve la app?\n" +
                        "Esta aplicación permite llevar un registro de productos de una tienda. Puedes:\n" +
                        "Registrar productos con nombre, precio, descripción e imagen.\n" +
                        "Cargar imágenes desde galería o tomar fotos con la cámara.\n" +
                        "Personalizar el tema visual: modo claro/oscuro y color principal.\n" +
                        "Editar o eliminar productos en cualquier momento.\n\n" +
                        "FUNCIONES PRINCIPALES:\n" +
                        "(Boton con un +): Agregar un nuevo producto. Te lleva a un formulario para escribir nombre, precio, descripción e imagen.\n" +
                        "(Icono de lapiz): Editar un producto existente. Puedes cambiar sus datos e imagen.\n" +
                        "(Icono Basura): Eliminar un producto de forma permanente. Se mostrará una confirmación antes.\n" +
                        "(Icono Engranaje): Ir a Configuración. Allí puedes activar el modo oscuro y elegir un color personalizado para la app.\n" +
                        "(Icono Interrogacion): Ir a Ayuda, donde se muestra esta información.\n\n" +
                        "REGLAS IMPORTANTES\n" +
                        "No se pueden guardar productos con el mismo nombre.\n" +
                        "El precio debe ser un número válido y mayor a cero.\n" +
                        "Todos los campos marcados con * son obligatorios.\n",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AppProductos(preferences: ThemePreferences) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "listaProductos"
    ) {
        composable("listaProductos") {
            ListaProductos(navController)
        }
        composable("configuracion") {
            PantallaConfiguracion(navController, preferences)
        }
        composable("ayuda") {
            PantallaAyuda(navController)
        }
        composable("agregarProducto") {
            AgregarProducto(navController)
        }
        composable("editarProducto/{id}", arguments = listOf(navArgument("id") {
            type = NavType.IntType
        })) {
            val id = it.arguments?.getInt("id") ?: 0
            EditarProducto(id, navController)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarProducto(navController: NavController) {
    val context = LocalContext.current
    val dbHelper = remember { DBHelper(context) }
    var nombre by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var imgBase64 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showFullScreenImage by remember { mutableStateOf(false) }

    var cameraPermissionGranted by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    ) }

    // Uri para la imagen de la camara
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Launcher para la camara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            imageUri = cameraImageUri
            imgBase64 = convertImageToBase64(context, cameraImageUri!!)
        }
    }

    // Solicitar permiso de camara
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
        if (isGranted) {
            // Lanzar la camara cuando se concede el permiso
            createCameraImageUri(context)?.let { uri ->
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Se requiere permiso de cámara para tomar fotos")
            }
        }
    }

    // Launcher para la galeria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                imageUri = it
                imgBase64 = convertImageToBase64(context, it)
            }
        }
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(8.dp),
                snackbar = { data ->
                    Snackbar(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        action = {
                            data.visuals.actionLabel?.let {
                                TextButton(onClick = { data.performAction() }) {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.inversePrimary
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Agregar Producto",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { padding ->

        // Pantalla completa
        if (showFullScreenImage && imgBase64.isNotEmpty()) {
            val bitmap = remember(imgBase64) {
                try {
                    val pureBase64 = imgBase64.substringAfter("base64,")
                    val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                } catch (e: Exception) {
                    null
                }
            }

            Dialog(
                onDismissRequest = { showFullScreenImage = false },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { showFullScreenImage = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (bitmap != null) {
                            Image(
                                painter = BitmapPainter(bitmap.asImageBitmap()),
                                contentDescription = "Imagen del producto en pantalla completa",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.8f),
                                contentScale = ContentScale.Fit
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        // Guardar la imagen
                                        bitmap?.let {
                                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                            val fileName = "Producto_${timeStamp}.jpg"

                                            try {
                                                val contentValues = ContentValues().apply {
                                                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                                                    }
                                                }

                                                val uri = context.contentResolver.insert(
                                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                    contentValues
                                                )

                                                uri?.let { imageUri ->
                                                    context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                                    }

                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = "Imagen guardada",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Error al guardar: ${e.message}",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        }
                                        showFullScreenImage = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Guardar")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guardar imagen")
                                }
                            }
                        } else {
                            Text("No se pudo cargar la imagen", color = Color.White)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (error.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Tarjeta para la imagen
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (imgBase64.isNotEmpty()) {
                                    showFullScreenImage = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imgBase64.isNotEmpty()) {
                            val bitmap = remember(imgBase64) {
                                try {
                                    val pureBase64 = imgBase64.substringAfter("base64,")
                                    val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (bitmap != null) {
                                Image(
                                    painter = BitmapPainter(bitmap.asImageBitmap()),
                                    contentDescription = "Imagen del producto",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("Error al cargar imagen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Sin imagen",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Sin imagen",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Botones para imagen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Boton para seleccionar de galeria
                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Galería",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Galería",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Boton para tomar foto
                    Button(
                        onClick = {
                            if (cameraPermissionGranted) {
                                createCameraImageUri(context)?.let { uri ->
                                    cameraImageUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Cámara",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cámara",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Campos de texto
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del producto*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = "Nombre",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true
                )

                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = "Precio",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Descripción",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    maxLines = 5
                )

                // Boton agregar
                val defaultImage = "/9j/4AAQSkZJRgABAQAAAQABAAD/4gHYSUNDX1BST0ZJTEUAAQEAAAHIAAAAAAQwAABtbnRyUkdC\n" +
                        "IFhZWiAH4AABAAEAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAA\n" +
                        "AADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlk\n" +
                        "ZXNjAAAA8AAAACRyWFlaAAABFAAAABRnWFlaAAABKAAAABRiWFlaAAABPAAAABR3dHB0AAABUAAA\n" +
                        "ABRyVFJDAAABZAAAAChnVFJDAAABZAAAAChiVFJDAAABZAAAAChjcHJ0AAABjAAAADxtbHVjAAAA\n" +
                        "AAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBHAEJYWVogAAAAAAAAb6IAADj1AAADkFhZWiAAAAAA\n" +
                        "AABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9YWVogAAAAAAAA9tYAAQAAAADTLXBhcmEA\n" +
                        "AAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABtbHVjAAAAAAAAAAEAAAAMZW5VUwAA\n" +
                        "ACAAAAAcAEcAbwBvAGcAbABlACAASQBuAGMALgAgADIAMAAxADb/2wBDAAEBAQEBAQEBAQEBAQEB\n" +
                        "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEB\n" +
                        "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n" +
                        "AQEBAQH/wAARCABkAGQDAREAAhEBAxEB/8QAHgABAAIDAQADAQAAAAAAAAAAAAkKBQgLBwECBAb/\n" +
                        "xAA3EAABBAICAQADDwQDAQAAAAAFAwQGBwACAQgJFBVYChMWGBkaIThSeJKYp7fRUXHX2BEXkYH/\n" +
                        "xAAXAQEBAQEAAAAAAAAAAAAAAAAAAwIE/8QAJBEBAAIBAgUFAQAAAAAAAAAAAAIREiFhAxOBoeEx\n" +
                        "MkFRwZH/2gAMAwEAAhEDEQA/AL2GAwGAwGAwGAwGAwGAwGAwGAwGAwGAwGAwGB5aLu+nDlsSaiA1\n" +
                        "pQEpdMLAs5RLqqHyoK7n0aj7/Zjw2LG4sg83MDWm+pYIupu6aJcoNJBHHjjVJpIgiz8PUsBgMBgM\n" +
                        "BgMBgMBgMCCby1+Wn4qno3Vfqw23sru7ZXqcCKFAQvEy4pvaZbN20adv4uixKfC23JVoRHLVjWHL\n" +
                        "B5rumUFTeYsXUdcxSKWXmUsa0uxkPEP4rCnUNA92e7LmiM97p3MwMLy94VkasrbVkMlpdtIz4XeR\n" +
                        "ckiac6s+UFW6JGyrFcPCKOjzlaKwx4uH0kkvsdGON63YnJzQYDAYFc3u77ofrPqt2AmtB1t19J30\n" +
                        "9rIq5i88mDq1WFax9pNGGjb1zHY4i0glkPzfMafqvI5JHhROM7sZSGLDmLIqOQbGXc+Zt38DUT51\n" +
                        "Iv7ByH5o1f8AXTHM27+A+dSL+wch+aNX/XTHM27+A+dSL+wch+aNX/XTHM27+A+dSL+wch+aNX/X\n" +
                        "THM27+BkQ/uow9IS4qPx/wAfLs6eOk2AUIEDdlX5QuZMFXaLAYKFDGPW9d6QJEXrhBmxYtEFnLpy\n" +
                        "skggluqprry5m3fwNq+/nmYtKiqvhdEV9UwePeRq6RAlovT0NlnN+69avh96O3hzaSOGMHjjKb3x\n" +
                        "ImRUepDKpFhCg0aTespLJVpHFVYiFtDUpY1pdiJyuLQp3wtToXZV+QlPuh5ObG9Yy2zg69rIthPU\n" +
                        "4TNGbwiQFl7NWjVmcS3sVYGxflewjjNm+WbA3pAQGkTKNlHUiuqcZY3pdi2x0e7m1h3w6/gL8q5q\n" +
                        "VCsnhUvF5XDz6g9U/B5mA3Q9aR0ssLdO2LjlZg9EyMK8RUT3IReQACLxkJIu3gYdSMsr0qht5mgw\n" +
                        "GAwOXH3744473d2eOPa77K/vRNc5xs14v/F1NfJRLrFaD7GH1JXtUs47vL5q6i68zJLGJbyc2joA\n" +
                        "BGtT0VbkXDhGNlnJZ4vI2KQRpozWUQerEWLRxqMcr1qh/Q+UPxMT/wAbTiuD+1jtrnquzFjIdhNm\n" +
                        "kLIQl/GpgFTQf7xiTBtjMsEN+DAZ16fFSLaVrvD2wOX8LgQ7cG3XKJRxrW7ESGZGTCBDMlMiY7HR\n" +
                        "RI9IDxJiGBgw7JySLmTBR0kyGihQ5mms8IEiDxdFoxYtEVXLt0sk3QSUVU005CdaOR0L4lwoAACj\n" +
                        "7G5fL7crEMIhkLFCG9gh+kgiw2zZrH0kwLLUsynPaecszLRpFow1ZGWghuWbOXDZ3FXbBjeFPZvf\n" +
                        "T0/v2PpIZIF8SoqQmjp0Zcnl9uAUSLTKZGSbSwxPSMPYiDh5IN1Dz/Yw1m/aidMCq7mUyhw8JNg6\n" +
                        "BJ42bOyURfPSF56lLGtLsQWGjRiSGCshkJUidPniT4ycNmHrkkWMFyblV6SKlCLxRZ4QJEHi6zt8\n" +
                        "+drKunbpZVw4VUWU332iLxXuY3jj4iNw8/17dzn/AJ/+UvQWU4fz0/RYzygYDAYHLk7+fXv7s/e7\n" +
                        "7K/vRNc5xun4ivKdr435vYoybV+SsWmLj0iykuQjL9JpOoadh25nQVJYkzLvWsaPIPxkgLDJDGyb\n" +
                        "kA4JrIRgizlwhEA/FSTUZY3pdjP+Xby1L+RctBoLXcLN13QNZk38jEjpW6YLzSdTMkw4GpSqVsRD\n" +
                        "kgDjmkcErEwkbj4cue5TTNSIsUkJHYyNDxVKWVaVQhrChTEkMCo9HhRE6fPEmIYGEDsnJIsYLk3K\n" +
                        "TIaKFjmaazwgSIPF0WjFi0RVdO3SyTdukosppptkTqx6MhvEqKjoQGAG3J5friFjQ8MhYYW2sMV0\n" +
                        "jEWKg2ZAEtI+x1MNpt2pnTEs3bRWLt2RFuHbk2TpwzJRB+xYXnqUca1ux+iWF2PiJEmjcoMN7b8w\n" +
                        "F3BiMmlkxPE052J6Uxuzmrh6VfevX2xNrPeztiMCz59Ipau+MMRrQo8bNXj2JvHz68Neze+np/fs\n" +
                        "QRGjRiSGS0ikRUkePniT4ycOGHrkmXMlyblV6SKlST1VZ4QIkHiyzt8+drKuXblZVddVRVTffmYx\n" +
                        "mBed9zG/UQuD73c5/Zegspw/np+ixnlAwGAwOXJ38+vf3Z+932V/eia5zjUrAnV8TvhhlnfHVzcN\n" +
                        "2P5hVfVxnoXGBD8c1GD55bMnb+ljN0K6WkQg4JYxOJlkt9pPOiQA2LfGB6sDjY8mW1l5mvdRjlet\n" +
                        "UPTJTXdf+GSWyCtqqNgO2nlJn5wnCajKRuHuZBH+rcHlrv1PA5S2rxyme2Odp7UBExpCN19trIm8\n" +
                        "VRNoIuXB6HrDtb7pGON63YnW8SfiU+Kdyt2m7SLaWR3bshIgeIlThTmYf9L/AAx5WISNiNkzl2/+\n" +
                        "FNtyn09xpZ9ncru9+NnROGQom5jruWSqy0Y43rdjY3yf+L6sPIjWeu/HIyD9iYOKdJ1Ra+6CuiKi\n" +
                        "euzp7pX1hegt3D0vXpYg4WWRWSbPTEGLvHEkjbd4k7k0YlyUcq1qhzy7opizevVmy6nbhiROEWJB\n" +
                        "yigqQx4ppp76ipxrou0fMnSG6rMoGLsVWxUEcGruhJwO8ZFhLx4OeNnKsR5fgXnfcxv1ELg+93Of\n" +
                        "2XoLKcP56fosZ5QMBgMDlyd/Pr392fvd9lf3omuc4lG8O/hyO9zTQzsD2CGFY51RAEluBYzhV4HP\n" +
                        "X6aFO/eXYCOvG+7Z+Mrge8QXHzWcjl0Xj50i9hUIeIyNGRSavtRjletUJ5vKb5SgfSoHHumPTGOD\n" +
                        "ZP25lAyMwaFwmBxVmcB9fw55uyFQdk1gQoY9Ym7HMj3YdGoKfYiVmTZg+By2TilYurEYhZNJSxrS\n" +
                        "7GW8SniW+Kp6T2o7Tud7K7u2T64PFSp41xMuKb2mXLlxJWjCULPivwutuVaEiKNn2dw+ea7pFC0I\n" +
                        "hz51HXMqldloxxvW7E7OaDAiy8oPi/rPyIVjxsnsLg/YmEC3OlU2so1U4RWS02dPdK8sPliiq/LV\n" +
                        "4UIuV3CKqKLwtBjLxzJY02eJvJPGJdmUcq1qhzzbopezuvNnS+nbiiBWD2JBiqomQR8slrqokpxr\n" +
                        "qs0fMXaOyrIuFLMVWxQGdFOXgc4IeMiwh89HPGzpWIume5jfqIXB97uc/svQWU4fz0/RYzygYDAY\n" +
                        "HNU8nvXq5qm8iXYkDJ4FIvWlwdh7CsSpfRI4TKM7IC2zPXkviKUK1UFqN5kQ44louNGRQVErswmK\n" +
                        "JKKutNijJdvxzibmf+RnuX0JoOOdY5dazC8/InboCHRiD0VXVSUsOgnSCHHBjQVAo0pGqcrUClYF\n" +
                        "+nhz8a3hNXIKkonH9No8Uex8lGG8ea3PaUsa0uxoPIJeJ8RbAnJS0lB3L5f7cZvj8umpwkJtAH0g\n" +
                        "GTlBy+ObuixlSQhrE7STdsVXUlZ9+odGC2hJ2z1UJQ98Re3nn2b309P79jwP5eDyqe1p+iXWf/Dm\n" +
                        "OZt38B8vB5VPa0/RLrP/AIcxzNu/gPl4PKp7Wn6JdZ/8OY5m3fwHy8HlU9rT9Eus/wDhzHM27+Bp\n" +
                        "v2f7tdju5xeLn+yliBbJkMNHPBEePp1rUMIPNQ7xx6aoHeGq6gkRKmhDd5s4ejBZt4RYB3hAs7Et\n" +
                        "2TgyWUezF1j3PLRVoUf0IJK2jFisNeW5eUztiJgz44gHP6Qx5Dq6ggwiWDlGjJ8N5NP4EWMhdVke\n" +
                        "dCkXfgJCzUVHGWau9OH89P0TrZQMBgMCCXy1eWvjqn6P1Y6tNdrI7vWOsEAihQMNvMOKb4l+rZKN\n" +
                        "vCEZSaveJXbMt5JjNqurPRkT4V4fj5hMhq8fdRWLWTmUsa0ux8+JPxKcdUff+03aVbmx+7dkolDh\n" +
                        "IoeK6y7emOJlyu8kbMfJ1nRHeT25K0n7pK07M4fvdt1CBiFxEm8jriUyyy9Cdn6P6a8/31455/8A\n" +
                        "eeMB9H2dfw6/xgPo+zr+HX+MB9H2dfw6/wAYD6Ps6/h1/jAfR/TXj+2vHHP/ALxxgMBgMBgeWXiK\n" +
                        "tg5TVqBaIkwCGXQXr+WjKtlkpYcEo/HJ2+CPW0ZMlGm7Esls2YFVWzj39wDkTRmono8eRqSNW6wI\n" +
                        "gERPir8Q7fqAUNdluzZ8fcvdSdEpISeTBUqWlYqs2spVd7H1o9IpC3ZmJPZs71fkHFkWUXa+sdkC\n" +
                        "b6GxlXQM6l8isYJycBgMBgMBgMBgMBgMBgMBgMBgMBgMBgMBgMBgMBgMBgMBgMBgMBgMBgf/2Q==\n"

                val ketchupImage = "/9j/4AAQSkZJRgABAQAAAQABAAD/4gHYSUNDX1BST0ZJTEUAAQEAAAHIAAAAAAQwAABtbnRyUkdC\n" +
                        "IFhZWiAH4AABAAEAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAA\n" +
                        "AADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlk\n" +
                        "ZXNjAAAA8AAAACRyWFlaAAABFAAAABRnWFlaAAABKAAAABRiWFlaAAABPAAAABR3dHB0AAABUAAA\n" +
                        "ABRyVFJDAAABZAAAAChnVFJDAAABZAAAAChiVFJDAAABZAAAAChjcHJ0AAABjAAAADxtbHVjAAAA\n" +
                        "AAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBHAEJYWVogAAAAAAAAb6IAADj1AAADkFhZWiAAAAAA\n" +
                        "AABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9YWVogAAAAAAAA9tYAAQAAAADTLXBhcmEA\n" +
                        "AAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABtbHVjAAAAAAAAAAEAAAAMZW5VUwAA\n" +
                        "ACAAAAAcAEcAbwBvAGcAbABlACAASQBuAGMALgAgADIAMAAxADb/2wBDAAEBAQEBAQEBAQEBAQEB\n" +
                        "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEB\n" +
                        "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n" +
                        "AQEBAQH/wAARCABkAGQDAREAAhEBAxEB/8QAHwABAAEDBQEBAAAAAAAAAAAAAAgHCQoBAwQFBgsC\n" +
                        "/8QANRAAAAYCAgEDAwIEBAcAAAAAAQIDBAUGBwgAERIJEyEUFTEWQQoiMlEXI2FxGSZCgZGxwf/E\n" +
                        "AB4BAQABBQEBAQEAAAAAAAAAAAAIBAUGBwkCAwEK/8QANhEAAgIBAwMDAwMCBQMFAAAAAQIDBAUA\n" +
                        "BhEHEiEIEzEUIkEJFVEyYRYXIyRxJUJSYnKho7H/2gAMAwEAAhEDEQA/AM/jjTTjTTjTUdtiNpMR\n" +
                        "av12PseVJlwwbysg2jo9oxRQVcqKOViIfVu13rphFxMY3OoUXMpMSLBin/QC51hKkbF9ybwwe1Y4\n" +
                        "Hy1hkezIscUUSB34ZlQySMzJDBErMoMk0salmVVJYga2j0t6O776x5e1hdjYyPIXKVKxdnEsxj7k\n" +
                        "rxNMa9SCKOe7kb0qIxho4+pasuAXMaxhnFOIrejB821avY22Uddq9SIu0OXKmNXArpKEA6ZkzR1l\n" +
                        "ft1PIo9gKThQo9D4mNyjj33gpkSSG1VkRwrKVv0m5DAEH7J255B/HP4/kc5Ff9PPU/GTTQXtq7or\n" +
                        "SV5HinE20dzwCJ0cowkWzi4JU4YfEkSOBxyo58c9xupiVoAGVkoFYhhApVG2SMWFIZQxTmBMv3C6\n" +
                        "R6gnECCIAKZe+hEB6ARD2298MvA92Ik/j62gp+fx7llAf7eeSfwPnVHB0J39Y5EeGzIKr3N3bX3U\n" +
                        "4UePLGthbBC+fLAEL+SORr8Od38JM/aB9PwMcoqAGBOQyPiRuYAERKAgBb+qJwEwCACmU4dgYPyH\n" +
                        "Pwb4wf5s10/91/H/AD+B4tH5/kAj8Hg69xdAOpk5b6bbWdtKp47622N1SgngH4OERl458hwpAIJH\n" +
                        "Hx0stv7r7CMXknIXakJsWDc7lyZHJ2OHDsqRQAQBNonZvJZY4mIRNFNUyiqhyJpgY5wDnzm3/t2v\n" +
                        "DJPNeqJHECX5v0uQB/A9/g8ngKeR3EjjVzoemvq3k7VejT2buqS1alWGFG2luWONnY8ctKcZ2oi+\n" +
                        "Wd3CqiqzMQBrZ1x9RHWLaC7S2M8fWuSi8jRiTx4hS7nFfYJeeiWPRnExWFSuXsXPMiIiDwyLCQPK\n" +
                        "IsO37mOQZlMuHx2v1J2pu25NjsVecZCISOKlmP2pJ4ojw89V1aSGxGo4ZvblMiKQ0kaA86uXVz0p\n" +
                        "dauim3MXvHee24n2nlJIao3DhbsWUoY3JTr3Jis0qLFbxN4tzEguVY6006tBWszTKU1OTmeajhpx\n" +
                        "ppxppxppxppxppxprG69d7NVKxxrrsivkC3x1WI5isLUzH7N4d0d7cLG+v7G22CCgmrVFc6jyMr1\n" +
                        "SWl3q7kqDJBBZukq6TVet0l42dUILubu7kxtZe+dY9v1qUJJ5mcWvq7KoeCq9kEIlkZuOF45PJj5\n" +
                        "6L+ibLY/Y2+ele8LsDvjsbc6gZnc1uJY+3F0G29Lt/CXbBkZSws5bKLRrRw988kqymOKT2W7MHqv\n" +
                        "7J48eidAj50oPmImXUImp8dl68hOuIlAwddAXx/H47+eaMn2bnarBpqTKCFParA+OeOO3tH8eTyA\n" +
                        "AfPHk67W47r1sfcErmhuSMhSVPM54Dck/c3KqpYnwAG8ceSNe/QznQ1SeJJ9IhRHsAO1X776/ACm\n" +
                        "UwD8fkAMYfgPjoAEbXLgMkrsxx7Hn8MEPA5/l+38/H/B+fk5PD1GwknLRZ+q4+AVtsRxwDx3dxAI\n" +
                        "5H8Ecjx5413BcqUEYd5Y5q5xlarTB2hGrWGXZzyrI8o6L7jeKj2kLDzE1LSJ0SmdKtYmLenYsiHf\n" +
                        "yAs2JTOOX/bvTzdO5pp4sZjVVa6d0s1qWOCqpYjtjEgLq0p8lUALBQWPCjnWqerPq06U9F6eOtb1\n" +
                        "3VK0+VmaGjicLHNl8vYijUtNcNKJovaowEKj2p5YozLIkURkkJUblRy/R63a4G+PbQifHTASya+Q\n" +
                        "028t+llGrn6iPK1bpFjhsa88d2ZRsFVCvDayAi5drwqMe0dO0bx/lFvVsi2KOPiWYJ7n1DT8UmTj\n" +
                        "kN9SEKnlgUCryxcEdvjnWCZH1++nebp1Nuxt43LFK5McbHhRj5/8Tx3lmBNeTCl+9O2EfVNZaQUz\n" +
                        "WKulhpGWNrjWBNq6bMb+adZowvZjz1TbZLxRUHtpYM5WNbOjSNuLUr/X3LGWYRczFSP6Vsq6K8fN\n" +
                        "R7B27ipJhLt27mFlI189qKO0tzdPN67Tt5KFI1nzFKKOzWm9+CWOeZa1uHvRV7WNeeVGR1AIctwV\n" +
                        "+4ajfrP0r9Qfph687UwOUlvyw7c3dmjishWlpZDHW6eNXL7euNUnLf6H7piIJYrEDyQiSMxGQTK0\n" +
                        "a/RVD8B/8/H/AG5NvXAHTjTTjTTjTTjTTjTTjTWDv/EmvmWSM71DC8u1B7AViKf5KKmcDAVSetxW\n" +
                        "dS6MUhgFQYxjj8RaHMAe0M1IeHfvnE0ReqWdyGM3xNDUsvWRUFp3j4DObEcVXsLMGBVRQBVeOFMj\n" +
                        "kfJB7RehDpjt3cnRXObmzuOrZKe7mIdsQRWkEiV6mCWXNmSMDhkexY3SvukEE/RwE8mNdWztVPRN\n" +
                        "pWUaNRs05vyiji3G2RfvzjHFKoNds+Wc4ZJZVeedVubdQtLhnUfA1OERmGTuMJZLZOoN0XDc6qkO\n" +
                        "s0VarOqePMpVp4+/uHPzxDMJPJh8LiMPJm9x5eOpKYLM1anTrkQV45uInuXZoa6vwzOoKhtW+of1\n" +
                        "G9K+hG6shsbCbPu7l3jjUi+qrtbyNHD0JLNavbhQe1LZtX3jrWq0s8UENepGsojORSVGRLwdT9HH\n" +
                        "0/6KxhkXOimS8g/fXbSNr9l2r2micRPrJIujooIow8FieJUQUVdOV0k20SaQWfmMsgmCjpQ3uKXO\n" +
                        "tkNxXIh+zdLdz2om57LW6d41sC0q+T7hpbeq5X21IHhZHVuSfjg8QeznrE6zZuy9jDbZ2rgoH4ES\n" +
                        "JgMVPIicqQGlzUW5pyyqOGJmjLFi3tjwuop7iekjZLLF0mz6C6sa/wBPiqs0kjTON1M2XLONVnFs\n" +
                        "hsKjOGyJFWpxJxatflFKxFUN0yjpIshDSlbcNLBGSkYosg1su1OnuWzsL3MbuLaA2urus9S1BuG5\n" +
                        "uKrZl4KTQzS3aFGehIqRxmJeJYJfvBeKTsWXVO4+o29eouVhyG9oaf1tSitKrYx1LGUa5rJPNN7c\n" +
                        "0GKxeKg94yTyMsrQSSOnCNL2xoojZLekJmkdV2F4aWjXyO2ljsny+TprVVOuZEQqH6BTq5MdSkk7\n" +
                        "npl05KjNxTNNKdmFGikXBx7cqYnkUpAqsjIbKOYxAn+lOUxws/TzWzWN2sLAq1+0WLJhMvuCCAuo\n" +
                        "nmK+3EWUSMpI1Zq/+7tQ0aoNi9ZdIq9OAGW3YkkkWKOOGugaaV3ldY0SNGZndVUFmAP51H0/vGHL\n" +
                        "Y1yBl4uNY23TWxOsz1vS8VGO/ptTi6HdnNVaKNZ5V7JDNyNhe355LvlUHsi3ZC0aihMOjP3EVBx/\n" +
                        "6m7yxeYzu1MHiZluHG7mxdy3eryLJWDmxFCK0bpykpCymSSRHZFKrH9zFuzp/wCkXpBubZm1Ore9\n" +
                        "t5458VU3d0d6g7ew+EyVeavlOIcBey4zFuvKIpKUfu4oVqdeZBNOsz2WWOIQNN9HfkkdcwNONNON\n" +
                        "NONNONNONNONNYlvqvaY3TZH1LsU0wthiqDA5YxVPNYK5zEbKS7P63GcbdshWFghExCZ30jIKRDl\n" +
                        "BOObJnR+ucg9SQOorHqomit1F2pbzPUeKvJPHQr5LGzyw3XSSdV/boLd2VFii4d5miB9uJSGZlft\n" +
                        "7ip463elL1B7e6R+k/fmZsYq9uK/sfeFbJ5PB0J4q9l8duk4HAUrQsSLMIoYbdKb6grBNJGJKa+3\n" +
                        "zciI97rVF0PXjD+s1Ko+WrVnm7UDYOdnIV1FYNyzRq+3wzmWOjGtmgGUpZIBJotHs7TGRl9UkTSr\n" +
                        "T3AeyRGrVUEAcOcBj3DtyKHp1JtnJZrcmYw+6pWE0ezty46nNtfckKx5CL6y/jo6hrVbcFDLiSSw\n" +
                        "i9sMnYG8E8yfU31023166pUOpG2dr3duNLUxWOu0bD2b0uQEEMtabITWxjqFZC0C42BYlVx7OPRy\n" +
                        "7mQoOqhfSZ2Nx9l2sZkm8/SOWHuOclr3GmJz9ZUyrOmqlRUvOO6ZCTMmbJeuU+tY7DgRlrXFTUpP\n" +
                        "y2WTDkjC05NXCTuNaszCNjpfnJRqiKlSQlox3GNo1RSeCygMVHyWCeB+OeOfGKY6tXuuEnydHGgN\n" +
                        "Eoa59aysH7u942qU7C9kPavf7jROwdfZWQ8gUsrfor5pqzSSlcXZKUl5iYwLijAU7A56rNcpED+n\n" +
                        "8UUjXrFCN+x1MVuybEf4cZ0d0HAkfH1m8ymI79Xan94NIJQchMwkW5X9rk1kPEtWWNQzOWZkJLcn\n" +
                        "7SofuZeWPjnx2j5+dfS/TqUwpr5jHZIswBSmmRV1Uh+ZObdGtE0YIC/bKZe5gfaChmE3Zx9jat2P\n" +
                        "ZXGNuueS8fuYjD2PdYcP2WZouZM0v3FBrraOeXmZsFvZxNjlrM/uSMTBQE7Pz06tYJF0wkJaRO+c\n" +
                        "KmA0VsluHDrvjqc+5Zs1hrUuFx+1Nu3q209xZiocdOzZPPXY5cTj7MHbflNOjKjWVkUUnDAqBzbe\n" +
                        "l/UPB9N+r+F37uHEWs3U2xkqNqtiIUtRC19KrWVmhtxUbsUUkV8VbA742VnpCJu0MxEGMr4bLQr7\n" +
                        "qRjqmWtrlWS2cvmv83i00HATkC8dwDnJLO5SdilYSdboykVA16hUO0XWUfro/wCRDxpgOgRZVsRe\n" +
                        "mxGz2lze0Gw2SizFHNQ4fclS2tW1RYY5rSTRSTVLY9+JpoomkVZOx+3+pU4512Vwfqk231M6L9Qe\n" +
                        "ohwF/bmLoYnc2CSG5citfXWsztGetRWtMsFRvdls5ulCYJK6MjSIe8hyq5g4fIfIdf6f2/8AHxya\n" +
                        "+uNenGmnGmnGmnGmnGmnGmrSnqo2qJxOOnucXgEaq442wxgvJSRfbTVLU5laQr1tanVMHkKKtZm5\n" +
                        "sgJmMCXuLAc4CBR5qbqdPHjLWzM0eFNTc1KOdx4JqWO+rZU+RyDWszrx/DH5+NSz9K+GtbwTrhsW\n" +
                        "L/UTcXRTdrQVmUukmWxL0c1iG7Rzw4yeLp/eB3BQQvHOrVlxyvl3B2aIrBsHk1zQcf129WylKPBP\n" +
                        "HMW8PFV/I1kjWarSQkhIyjjPaYSsv27qUVSjWbyXKu+VQjk1VCQjye691bP6hYfYlfccWA2vDmsr\n" +
                        "h5LMwrxmpXxubyMUUcVmy6QQSWcT+1iCWdzBHLbSSQ+2Cmub1u7cxtsUYr7Va9azNW+0ogVIrUsY\n" +
                        "KyOo7WMBiKtIxQd6s/28sN1rsbsdZ0wGq7SMG8qxokncLRDPJqIkmkErAwjiWlm36gSkI9JVIBSK\n" +
                        "xL9K1kQB0g9XScLNndfRl82qbh3nuWOSxg+q+OxM9PAfueZxlhq+arYe3Xima3HYzUGSpw+xHMqV\n" +
                        "5WEUojKTzxSyxNWWahObzsoDRZsqy1nt2IzIjLCqRCRkE3MauvJEYKK7FwxXvVoe/jF2S2GaSirR\n" +
                        "1uVSHyLEyTxwi0u1SZDIxjUMfuJVFtMOLBIM4yVctrjNta82dM3rp/K0WxtnEaioi2buvVe/1Biu\n" +
                        "RRW+tO3LQjmglenXajWe7ShkxSZCOK8c3bipWBJds1qgkgneWWq3dChbtVJns1GwQ5+J+37ifqoF\n" +
                        "7kDUxIFleZo0cfVOsSujl5ak6Mg+xD5hTMu4dkVs7eStduveOk0rRRnWTIaCcymLJ+6rRsjB1yEh\n" +
                        "skRgSVXlGsvaXMG2ZJllmcm9aPSIKRbJ4um1HF8hb66yZC97ti9nNnnGZGCbcNTGI2MsZJq9yjWr\n" +
                        "1MhDevC1TORNNVvdkJkSRklrVHUoKiOzuezDYlNm9cpotiH6pVV6c8hVokEM0fdFNC8zRqjqwLkg\n" +
                        "lEYsgn9haCrk16wFWrhU0X8PrRp1kunY6EwB1EytXkta8Yyr9EnZipOCMpu4xKQF+UmsvIJlEPqF\n" +
                        "gNLHZMVev1CvYeuQam2NrY/B0wo8KMXDj6j+AOFKvJYQgH5JBPPz053JtuXZ3os6dwRq9eTcu+qm\n" +
                        "RyCICqz1J8fuKTHe58dyyVMRip15+TFH4+0Fb9fN6ah1pxppxppxppxppxppxpqw5/EHlMbTauiU\n" +
                        "ehJlKumAe+uxAjgviUP+o38/l0A9+JTCHwBuaO67yBdtY4D+v91iZQOeeVHI4I8+eD8efGp9/p0J\n" +
                        "7nXHKoV7kfY2cRh29wKt7XcHHH9J4VRz4Jbj5I1Dq14oZ7bpwGTBlLUjLXPX/Cexh4uk1tjbbBZZ\n" +
                        "y61Kq4jyNFQ8Y+moNs4kIi54ucScmQ8mkcSqPnKSaqqwlUjpu3pPT6ub0khmy/7LLfwOD3lUsCD3\n" +
                        "1l/dMbVwOVrsnux9pju7bhnkcyHh52LqpfuPPXrT00GA60dSNll2x64Pc+VWoTF3rLTF6alWcJ3J\n" +
                        "wstanWnHH4lHH9XJj/Ea0PVmD2OZ1vd2IZTbto3lGjjWGOaN36Ma5dJxL9ydPJ50n8aLxZ0ZiRqo\n" +
                        "4ciBwfrsUmJ27w1VT9JNvH47JYar1Kmr43M/TfulOGj7a3kqu7wLOVt+52RtI57BIFPeQwIPB103\n" +
                        "Tiwe4jMlQwAI+nbtYr3doYe99wXvfjn/AMj4HPjq3elsCzj7IuGPdqn7aBeETUbsNU4FdxPPixMf\n" +
                        "LEGDjj5FK8klfB4RmRcWqSYvGL9Mq5k2SqnKpPSjlUmSZOqOS70ACO4uFkB5+wN+5DhPJPaTxyWJ\n" +
                        "8kk+f8uJyOJctG6hCo/2zr2glmPkzMOOTyT8/PP96lYI14yW2z7rm9QuchUscvJuZhpSIyLie+Yx\n" +
                        "z5W8b1OIsmSrCQYxTIkvjOPp8tBR87V4p6arjPtglVpFSMbTJIqePI/F5VNi4/YWw73/AFu/bglx\n" +
                        "LZKIrBX+jw+LuZC1lb6SPPM0j1qQE/BYWbs5cyIrt251jI2pDb+3IkF+5dlTG1xAvaHZFYhmQliW\n" +
                        "btCtwfMrct2htSf9OazO7n6lEreJADFc3HXLZCYcCoYBEz+dzhr1PKogPf8AMoUDuTiQvfimmYwd\n" +
                        "EAetddIcg+R35n7srM0mQp5a4e8ju4nylOYADkngd/A8ngDjk+QOtnrC20u1/TrsbA14ya+2d07E\n" +
                        "wReND7aipsvdtRGc8nhpTUJDE8se4n5OslPkntcr9ONNONNONNONNONNONNYm/8AESbGOXljx1rM\n" +
                        "ySOm1ikmWS5pcxg8FjrIysRDEbiUfn3TuJf61JQo+2aMj1UjD7qgEi11wzclrN4/byp/o1Io78ve\n" +
                        "T2tI4ljjIHjkdplDDk/0qR8kDsJ+mn0ygrYLenVyzMj2b0r7JxdYD7o44nx+Vy08hJPDBVxsdZ08\n" +
                        "slu4jAdgLd56S+UAlcXakSp3PmrTLvsJq3NkVMAqrFnRgM6Y9ETiIG9tmhZb00YeYiQyLVw0QDzQ\n" +
                        "MULHty+1fL9Msj3qI5P8XbHtDkgFplp7kw4fzxzHGmWWLy32llUjzzCX9Qraf+GPU+c4sTJV3xt+\n" +
                        "hk+EQCOSdKxxL9vHy5mwNqaQcgq1ksw5k5eW8ftTJ4ix7tnP0clxv+J9b8lPqPDJztvWdXo18fWu\n" +
                        "IhJymSNoyC1uJWNGqEhKvDRDgqTyUJGIxsKyjSrgz+85+dwT4uluyzHWnmxe37Rq0mtWLAuWL5cP\n" +
                        "ZjaW3BMi4yEzQJBOpnZlMiLGqJCJInemTY97rn1YfpNt7cUeGxr5zN4mvm80b+VuVc1QoWsnZxZr\n" +
                        "yJals1UaBa9Jo2KwQTLJOYq8DzN5zDGwGS9/9S85PTGueP0Iy1NqNbl4aQSRsLWvs21Zt87YsYzl\n" +
                        "Xhqg7eOUq29VSk8bWNlJqWNusSKNbCtZo0e4/cHl728dvX7DS2qZdnrP9LJC7oscaSSPjpq9eFmb\n" +
                        "glJa8wlkk7iq2Ii5K7I9X3pc3v0IipdPt173xmVym7dox57KPs+xNHb29VlzeQx8sdO2telLfkar\n" +
                        "jXnVJ69dbqyT029tIi83W2+iYz1iisl1vE7NBpUsHamSEahMpKIuJKzZN2itlfpjO+zksiHcrZ5S\n" +
                        "EZ2NwDkTiVvHlRQYpN4ZmybIYbmp61fdWYkpFfpdodOLkULBmkY5Le9+tjKc7Snw030lO+VbuLBG\n" +
                        "+3hPtOOekPpbiLXXbpHsbCQGbD4a/WytppZGsvbSjK24Ldy7Ydneea5WxFiOWVmCt7wiRViWNBBb\n" +
                        "XvJ8hhTOOEMwRggoWtXWOqVoaGEwFlaDlOSh6ZaGaZSAIndsF3MLZY8hjJpHfwLciogQwhzCdk5i\n" +
                        "fb+6sHkIeO03I8fcj54WWjkZErWF54bzG7RWowCOZIEBbtJB7zddtlY3f/S3qNtHIgQtZ25b3Dg7\n" +
                        "ZBZqG6Nm1bufxMpPgiG/FDkMHZYKe2vlZJFHMf25iQD2AD/fk6dfz961400400400400400401g7\n" +
                        "fxB6hkd6WHQH6Nhqm9fAgHYzdtAAAeyh110I9CPwYP3+OQ/6wosm/Jiv9UWKpeAOPuDWmP8AHyGH\n" +
                        "/JHP5PPdj9O1uz083GQgH/MDcHdzyeAcZtxQSOfHkNwQP+1vkEceD9KPJZoqlbC1g7wvv46tuv8A\n" +
                        "szCIKKin9CwqF6c4myXIom+AT/5Uy5GOHLgweKSMSQTgYpvEcSNsVtq37zFY22ruHae7IyxKLHUg\n" +
                        "yTYXMz93H2quKzM0kvHPcsXJHA8Ro/VS2sTg+lvUBY1D4fNX8FcsgcD2rprXaKyPx28RwwZpgPji\n" +
                        "Q8gjWQiTZzWw8hkIf1XqsSHyivLL5BZum7xc17VbRBIiuqXFmBFYmbkH0cDaJscq+YuXARkU2jiJ\n" +
                        "SjQjAY/ZDdXtjzGZZt17FavaFg2I3vCUzr4FRJVZ+wiRDxY7lbs8LF3rwRyLwm5IdvZb982/kqOF\n" +
                        "yq31ycWTxNh6GQTI1TGKV9blaaKeO9Gscf8Au43WeIQoqSOAnbu0rbbAWLYKOqeN7HrBS6axXcLL\n" +
                        "1elANajUVH8kYHDqJaR5Wces6M3N91kXDxkwVfKgLcoi4OKif5D1f2LTijgqbn2TBVXuU1a+QjiC\n" +
                        "d8qgGIrIkQHbzJJ3Ivd4A5POr/ujqhnN45abP7v3XY3Pm7fZHcye4Mxdy2SsxRLHFCj5C/Zs2Gjh\n" +
                        "iULGjkrEgEaoqqObcmdbsUusMrJIyDV2tl3N9Ip0e7j3aL1pKUXXPEcU1cmbPW4ii8ZhkG+uzILJ\n" +
                        "GOiT6ICpeRDFOGAVsjDkNu7nzlSaKxBuTd2Pw+PnrSe7DZw2z8FWXvhmQuk8AzGUthSrMhdS6szc\n" +
                        "kTd/S+2g2Z6obo3bKizRbW2hDQgscq8YvX/o8bDKkn3D769fPAlSCef4PBirECVdCtl7H5uuM1C/\n" +
                        "6mJkGqGAPkfjsxAAAAA7+f35ZMWvOVxn/pyOPYgePK3ISR55/jnkfP8Ab511z6hoU27uYg88bc3O\n" +
                        "PH9tu5QEeOfkN545/P8APGs3Mv4D/YP/AFzoBr+cPWvGmnGmnGmnGmnGmnGmsYr1/tQJbIA0nP8A\n" +
                        "UyJqTEJDLV58zOQpTzZU3aaicQ3XMYBUlPp1PrK9HidMz9y3lYlgk/m5qKZmjh1rwbC5TzcEfLS1\n" +
                        "XrSlBy0hiPcsY4HBkKsTGGPLfeiEM3B6efp9dXoNuNnen2WtJFi8nkochG8xYR4+aeIw/XuwBEdR\n" +
                        "5khr5CYqUrx/SWJWjghmfViPRrEO5LC7ub3p1kbG0DkVzBytYeMXdzxonZnlfk1mLqUjz0bJDB6W\n" +
                        "Vj1nEcwXUdIQjxFs6bIHTdJLph1rHZ4z00zTbcsU1u+1LTlgltYtbDQuVMiSUL5kMiMYlPcIW8jw\n" +
                        "QeeZ9+pCt0pXb1Xb/XDAZrK7Skt1cxTyVHB7rubea9HDOtSWPcO3BHVjsrBZsRmtLkI2ZJGLRMCr\n" +
                        "aufO4v1+4sFTvrjVRTAweChmenjRIwd/AJneVZn8iH48xTEeh+A66HYb0+pyeTDSjQKCS1Xa5HP9\n" +
                        "u+qPkHwOQOeAB+BDOttf9O+4QkW3LUzc8kR1uqcvA88krDalPPnz2hj/ABzySOXU2vrxSEmgeTuV\n" +
                        "UXjklFBeNkW+nrpQU/bN4AH2ivOFADyEo/KhDdh+4d90bxdUm5EVeiwB8sK20T4HPB7RACASPz8j\n" +
                        "+3OrjY2j+nXBDw217ld244klp9W4UB488CxaQPxwQewk+fHAGqT7EO9urHJVOK2YtdcsczVTybWv\n" +
                        "V6EmMUrzUZ95WaKyp1KtjMGzpBN0rHs/ffyMWRPybJEM6L4gQMH3VY3OBVTctms7VRMlSlBLhUeE\n" +
                        "WDG05FLG+3KA5SNmlkhIHCnvA+JJ9B8B0H27Tzc/RPD3sNjsz9JNmsjbxW9auMs/t62RUjXL7ojk\n" +
                        "omSJbthkq1LYmkEzO0TdoIpbglSdz5vfrHp/jZQsoaKyBWMwbES8ccHbOq4yxJNR90lq1KOUjHSZ\n" +
                        "up99GRVcdqCYRazNgr8SJjLPHzdK49PdvSZrcOMRoz7MFiK9ZLA8COpIlhQfAA9x40QclvJHA+7z\n" +
                        "hnqd6n0dndMd434Zgt3I4u/tzDd4KvYyGdrS4sSwBiWJggms2wPsJhrSP9qjznYh+A/2/bkzdcI9\n" +
                        "a8aacaacaacaacaacaapTm3DtOz1jK1YtvcejIV+0xq7JcqqZVRarnTOVu8SAeh9xuc3l0UxBOTz\n" +
                        "T8yeXkFpzeHqZ3HT462oKSjujf8A7oZl8xyof/JG8/kEcggg6ynZm7stsbcmL3NhpfbuY2wkpjJI\n" +
                        "iswdw96rMB8xTICp/Kt2yL9yDXzovUk0wzHpHn6ej2FgsLWHmXrmeqz1dRY8LLNxWEyoNXHtFT94\n" +
                        "nuoKA4blKql7qQrpEdJKk5FvJbQjx2UlqZWm6TLJzFbjUxrMhLdk0co4BDAck89yt3jwR267H9OP\n" +
                        "UPnM7tKjltl7maKu0CQ3dt2J0miqSxoEepPjpHYJEhBVOI/akXtMYaLgmE8Rka9WxkEhPz8mdYi3\n" +
                        "0i7eYkXii7ZVIPwm5Ksoi5an780VSkQKUg9KEKoBiltt/aK9olSb3V8DtsPI7Ac8AB2LccKeOPPk\n" +
                        "Hjx41tvavqUylcGLK4yvA4Zi02Iir4wEkAlmihRFJPHHhkbn+ryRxqpdJaLepN2sxILOxUBYhopR\n" +
                        "04FLxATlMLoxitk/gBL/AJYLHDvoSeI/Fvj2uQ/Le2qnjyjsSAfB8ggt/HxwfDHkDWZ2vUXatwOm\n" +
                        "PqWppCAgN+f3oRz5PdGzlHHd/SO0j48gc8VdoeRdv8yz8djfGryZRUnXSEayrOOa0d9cp07hdNsk\n" +
                        "kd4zQWfpqOTqpIKKsU45FMDlBUxSeRzV9TbNIssNOtYu2ncKqJ3v9x4U8IB54bgdx5A55Y8fOrd1\n" +
                        "9Tsm1SbO7lzmLw+KqV2Z3kaCCOOKNSzqHkdiCVV2CRr7njtQFvBznPR99LuC9PrFc9aroiwmdl80\n" +
                        "kYyWUrAicj5OqxDcx3cRjKAkezgqwiHC6shZZFof2bDZlDqgs8iYevGQktsnaUW2cfzKqHJWwslt\n" +
                        "1AIh8HtrRsPlI+eXYD75Cx5KhOOQvXvrNc6tblX6WSxFtXCvLDhKsxZZLUjkLPl7cXPC2LQVUgjb\n" +
                        "lq1VUTkSST915EA6+A5m+tC6caacaacaacaacaacaacaattbA6/YOzrZpRLMuKaLk4sZMPzsCXav\n" +
                        "sLCVmcgnapi3JIIrAiYrcvsEMn4mIiHtlHw8i8+ckMUy9s0Ucq889siK68j4PDAjkfzqorW7VOT3\n" +
                        "almxVl449ytNJBJx/HfEytx/bnjUWj+lToDKgZcupeEGnuKGUE6dcQjQUOJhEwim0VT8i+XfYeHh\n" +
                        "2AgH9IAFMcbjj80KR/5qwH/9j1d4917piHEW5M/GB54TMZBRz8c8CwB8a2v+E1oU3ATl1Rwu6KPQ\n" +
                        "nMhDfU+BeugEU1XBTAAD8AIFEv7j2HiI+DicUfnGY88/PNOsef8A69ff/Gu8QOBuzcoH8DOZMD/4\n" +
                        "tarfivR3WXE040kMX4FxpQ5IPpkBlq3Vo+MfA3QVQcEIZ23TK4OCKqKSqYKKG8VkyKB0YvkFRDTq\n" +
                        "VjzXq14CR2kwwRREr4PbyiqeOQDx8eB/GrRfy2WygQZLKZHIKj+4q3rtm2FfgjvUTyyBW4JHcBzw\n" +
                        "SOeCdXXEQ6SSDsR6TIAiIiIiPiHYiI/IiP5ER+RHsR5UaoNbnGmnGmnGmnGmnGmnGmnGmnGmomXy\n" +
                        "qW9taJZ7HQEhIR712o8RcR6ZXgGKuPuKFFFIVF01CKnUAQOmTyEOyiYolNxprx5Im6nEOoGwI9dA\n" +
                        "mVxVpgUwAPnoyyTVT4777HwAB/q+RH5aa9LE167GOUzpi4ITyL5AhBSnmYoj/MURdNkAKJg7AB8T\n" +
                        "+PfYkH8C01VOHr0v7qIqRy6Rey+R1yFREP5i+QmBQSD+Og66+fx8jxpqtQB0AB/YADjTWvGmnGmn\n" +
                        "GmnGmnGmnGmnGmnGmnGmnGmnGmnGmnGmnGmnGmnGmnGmv//Z\n"
                Button(
                    onClick = {
                        when {
                            nombre.isEmpty() -> error = "El nombre es obligatorio"
                            precio.isEmpty() -> error = "El precio es obligatorio"
                            descripcion.isEmpty() -> error = "La descripción es obligatoria"
                            precio.toDoubleOrNull() == null -> error = "Ingrese un precio válido"
                            precio.toDoubleOrNull()?.let { it <= 0 } ?: false -> error = "El precio debe ser mayor a 0"
                            else -> {
                                error = ""
                                val db = dbHelper.writableDatabase
                                try {
                                    val values = ContentValues().apply {
                                        put("nombre", nombre.trim())
                                        put("precio", precio.toDouble())
                                        put("descripcion", descripcion.trim())
                                        val imagenFinal = if (imgBase64.isNotEmpty()) imgBase64 else defaultImage
                                        put("imagen", imagenFinal)
                                    }

                                    val newRowId = db.insert("producto", null, values)
                                    db.close()

                                    if (newRowId == -1L) {
                                        error = "Ya existe un producto con ese nombre"
                                    } else {
                                        error = ""
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Producto agregado correctamente")
                                        }
                                        navController.popBackStack()
                                        }

                                } catch (e: Exception) {
                                    db.close()
                                    error = "Error al guardar: ${e.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Guardar",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Guardar Producto",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Funcion para crear un URI para la imagen de la camara
fun createCameraImageUri(context: Context): Uri? {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile(
        imageFileName,  /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarProducto(id: Int, navController: NavController) {
    val context = LocalContext.current
    val dbHelper = remember { DBHelper(context) }
    var producto by remember { mutableStateOf<Producto?>(null) }
    var nombre by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var imgBase64 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPermissionGranted by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    ) }
    // Control para visualizacion en pantalla completa
    var showFullScreenImage by remember { mutableStateOf(false) }

    // Para control del Snackbar de error
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Estado para mostrar mensaje de error en SnackBar
    LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Launcher para la galeria de imagenes
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                imageUri = it
                imgBase64 = convertImageToBase64(context, it)
            }
        }
    )

    // Launcher para la camara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && cameraImageUri != null) {
                imageUri = cameraImageUri
                imgBase64 = convertImageToBase64(context, cameraImageUri!!)
            }
        }
    )

    // Permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            cameraPermissionGranted = isGranted
            if (isGranted) {
                cameraImageUri = createCameraImageUri(context)
                cameraImageUri?.let { uri ->
                    cameraLauncher.launch(uri)
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Permiso de cámara denegado",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    )

    // Cargar datos del producto
    LaunchedEffect(id) {
        val db = dbHelper.readableDatabase
        try {
            val cursor = db.query(
                "producto",
                arrayOf("id_producto", "nombre", "precio", "descripcion", "imagen"),
                "id_producto = ?",
                arrayOf(id.toString()),
                null, null, null
            )

            if (cursor.moveToFirst()) {
                producto = Producto(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id_producto")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio")),
                    descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                    imgBase64 = cursor.getString(cursor.getColumnIndexOrThrow("imagen"))
                )
                nombre = producto?.nombre ?: ""
                precio = producto?.precio?.toString() ?: ""
                descripcion = producto?.descripcion ?: ""
                imgBase64 = producto?.imgBase64 ?: ""
            }
            cursor.close()
        } finally {
            db.close()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(8.dp),
                snackbar = { data ->
                    Snackbar(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        action = {
                            data.visuals.actionLabel?.let {
                                TextButton(onClick = { data.performAction() }) {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.inversePrimary
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar Producto",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { padding ->

        // Funcion para mostrar la imagen en pantalla completa
        if (showFullScreenImage && imgBase64.isNotEmpty()) {
            val bitmap = remember(imgBase64) {
                try {
                    val pureBase64 = imgBase64.substringAfter("base64,")
                    val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                } catch (e: Exception) {
                    null
                }
            }

            Dialog(
                onDismissRequest = { showFullScreenImage = false },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { showFullScreenImage = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (bitmap != null) {
                            Image(
                                painter = BitmapPainter(bitmap.asImageBitmap()),
                                contentDescription = "Imagen del producto en pantalla completa",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.8f),
                                contentScale = ContentScale.Fit
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        // Guardar la imagen en la galeria
                                        bitmap?.let {
                                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                            val fileName = "Producto_${timeStamp}.jpg"

                                            try {
                                                val contentValues = ContentValues().apply {
                                                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                                                    }
                                                }

                                                val uri = context.contentResolver.insert(
                                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                    contentValues
                                                )

                                                uri?.let { imageUri ->
                                                    context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                                    }

                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = "Imagen guardada en la galería",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Error al guardar: ${e.message}",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        }
                                        showFullScreenImage = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Guardar")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guardar en Galería")
                                }
                            }
                        } else {
                            Text("No se pudo cargar la imagen", color = Color.White)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mostrar el error como tarjeta (igual que en AgregarProducto)
                if (error.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Tarjeta para la imagen
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (imgBase64.isNotEmpty()) {
                                    showFullScreenImage = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imgBase64.isNotEmpty()) {
                            val bitmap = remember(imgBase64) {
                                try {
                                    val pureBase64 = imgBase64.substringAfter("base64,")
                                    val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (bitmap != null) {
                                Image(
                                    painter = BitmapPainter(bitmap.asImageBitmap()),
                                    contentDescription = "Imagen del producto",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("Error al cargar imagen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Sin imagen",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Sin imagen",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Botones para imagen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Boton para seleccionar de galeria
                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Galería",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Galería",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Boton para tomar foto
                    Button(
                        onClick = {
                            if (cameraPermissionGranted) {
                                createCameraImageUri(context)?.let { uri ->
                                    cameraImageUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Cámara",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cámara",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Campos de texto
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del producto*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = "Nombre",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true
                )

                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = "Precio",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Descripción",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    maxLines = 5
                )

                // Boton guardar cambios
                Button(
                    onClick = {
                        when {
                            nombre.isEmpty() -> error = "El nombre es obligatorio"
                            precio.isEmpty() -> error = "El precio es obligatorio"
                            descripcion.isEmpty() -> error = "La descripción es obligatoria"
                            precio.toDoubleOrNull() == null -> error = "Ingrese un precio válido"
                            precio.toDoubleOrNull()?.let { it <= 0 } ?: false -> error = "El precio debe ser mayor a 0"
                            else -> {
                                error = ""
                                val db = dbHelper.writableDatabase
                                try {
                                    val values = ContentValues().apply {
                                        put("nombre", nombre)
                                        put("precio", precio.toDouble())
                                        put("descripcion", descripcion)
                                        if (imgBase64.isNotEmpty()) {
                                            put("imagen", imgBase64)
                                        }
                                    }

                                    db.update(
                                        "producto",
                                        values,
                                        "id_producto = ?",
                                        arrayOf(id.toString())
                                    )
                                    db.close()

                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Producto actualizado correctamente",
                                            duration = SnackbarDuration.Short
                                        )

                                        navController.popBackStack()
                                    }
                                } catch (e: Exception) {
                                    db.close()
                                    error = "Error al guardar: ${e.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Guardar",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Guardar Cambios",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemProducto(
    producto: Producto,
    onEditClick: (Producto) -> Unit = {},
    onDeleteClick: (Producto) -> Unit = {}
) {
    val imageBitmap = remember(producto.imgBase64) {
        try {
            val pureBase64 = producto.imgBase64
                .substringAfter("base64,")
                .replace("\n", "")
                .replace(" ", "")

            if (pureBase64.isEmpty()) {
                Log.e("ImageLoading", "Cadena Base64 vacía después de limpieza")
                return@remember null
            }

            val decodedBytes = try {
                Base64.decode(pureBase64, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                Log.e("ImageLoading", "Error decodificando Base64: ${e.message}")
                return@remember null
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                inPreferredConfig = Bitmap.Config.RGB_565
                inJustDecodeBounds = false
            }

            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)?.asImageBitmap()
        } catch (e: Exception) {
            Log.e("ImageLoading", "Error general: ${e.message}")
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageBitmap != null) {
                Image(
                    painter = BitmapPainter(imageBitmap),
                    contentDescription = "Imagen de ${producto.nombre}",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error cargando imagen", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${"%.2f".format(producto.precio)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = producto.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Column {
                IconButton(
                    onClick = { onEditClick(producto) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { onDeleteClick(producto) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaProductos(navController: NavController) {
    val context = LocalContext.current
    val dbHelper = remember { DBHelper(context) }
    var productos by remember { mutableStateOf(emptyList<Producto>()) }
    var deleteDialogo by remember { mutableStateOf(false) }
    var productoDelete by remember { mutableStateOf<Producto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Función para cargar productos
    fun cargarProductos() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM producto", null)
        val lista = mutableListOf<Producto>()

        try {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id_producto"))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                val precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio"))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
                val imgBase64 = cursor.getString(cursor.getColumnIndexOrThrow("imagen"))

                lista.add(Producto(id, nombre, precio, descripcion, imgBase64))
            }
            productos = lista
        } finally {
            cursor.close()
            db.close()
        }
    }

    // Función para eliminar un producto
    fun deleteProducto(producto: Producto) {
        val db = dbHelper.writableDatabase
        try {
            db.delete(
                "producto",
                "id_producto = ?",
                arrayOf(producto.id.toString())
            )
            cargarProductos()
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Producto eliminado correctamente",
                    duration = SnackbarDuration.Short,
                    actionLabel = null
                )
            }
        } finally {
            db.close()
        }
    }

    // Cargar productos al inicio
    LaunchedEffect(Unit) {
        cargarProductos()
    }

    // Diálogo de confirmación para eliminar
    if (deleteDialogo && productoDelete != null) {
        AlertDialog(
            onDismissRequest = {
                deleteDialogo = false
                productoDelete = null
            },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro de que deseas eliminar el producto ${productoDelete?.nombre}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productoDelete?.let { deleteProducto(it) }
                        deleteDialogo = false
                        productoDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteDialogo = false
                        productoDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Productos Disponibles",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("configuracion") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                    IconButton(onClick = { navController.navigate("ayuda") }) {
                        Icon(Icons.Default.Help, contentDescription = "Ayuda")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("agregarProducto") },
                modifier = Modifier.padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 50.dp
                )
            ) {
                items(productos) { producto ->
                    ItemProducto(
                        producto = producto,
                        onEditClick = {
                            navController.navigate("editarProducto/${producto.id}")
                        },
                        onDeleteClick = {
                            productoDelete = producto
                            deleteDialogo = true
                        }
                    )
                }
            }
        }
    }
}

// Funcion para convertir una imagen a Base64
fun convertImageToBase64(context: Context, uri: Uri): String {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    val byteArrayOutputStream = ByteArrayOutputStream()

    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()

    return "data:image/jpeg;base64,${Base64.encodeToString(byteArray, Base64.DEFAULT)}"
}
@Composable
fun ProductosTheme(
    darkTheme: Boolean = false,
    primaryColor: Color = Color(0xFF6200EE),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme().copy(
            primary = primaryColor,
            primaryContainer = primaryColor,
            secondary = primaryColor,
            secondaryContainer = primaryColor
        )
    } else {
        lightColorScheme().copy(
            primary = primaryColor,
            primaryContainer = primaryColor,
            secondary = primaryColor,
            secondaryContainer = primaryColor
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
@Preview(showBackground = true)
@Composable
fun VistaPreviaListaProductos() {
    ProductosTheme {
        val navController = rememberNavController()
        ListaProductos(navController)
    }
}

@Preview(showBackground = true)
@Composable
fun VistaPreviaEditarProducto() {
    ProductosTheme {
        val navController = rememberNavController()
        EditarProducto(1, navController)
    }
}
