package com.example.appcalculadoradeimc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.OutlinedTextField


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavegacion()
        }
    }
}

@Composable
fun AppNavegacion() {
    val navControlador = rememberNavController()
    NavHost(navControlador, startDestination = "pantalla1") {
        composable("pantalla1") {
            Pantalla1(navControlador)
        }
        composable("pantalla2/{imc}/{categoria}") { backStackEntry ->
            Pantalla2(
                backStackEntry.arguments?.getString("imc") ?: "0.0",
                backStackEntry.arguments?.getString("categoria") ?: "Sin categoría"
            )
        }
    }
}

@Composable
fun Pantalla1(navControlador: NavController) {
    var estatura by rememberSaveable { mutableStateOf("") }
    var peso by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Calculadora de IMC:")

        OutlinedTextField(
            value = estatura,
            onValueChange = { estatura = it },
            label = { Text("Estatura (m):") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = peso,
            onValueChange = { peso = it },
            label = { Text("Peso (kg):") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val estaturaNum = estatura.toFloatOrNull()
                val pesoNum = peso.toFloatOrNull()

                if (estaturaNum != null && pesoNum != null && estaturaNum > 0) {
                    val imc = pesoNum / (estaturaNum * estaturaNum)
                    val imcTexto = "%.2f".format(imc)
                    val categoria = clasificarIMC(imc)
                    navControlador.navigate("pantalla2/$imcTexto/$categoria")
                }
            }
        ) {
            Text("Calcular")
        }
    }
}

@Composable
fun Pantalla2(imc: String, categoria: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Resultado IMC")
        Text(text = "Tu IMC es: $imc")
        Text(text = "Clasificación: $categoria")
    }
}

fun clasificarIMC(imc: Float): String {
    return when {
        imc <= 18.4 -> "Peso bajo"
        imc in 18.5..24.9 -> "Peso normal"
        imc in 25.0..29.9 -> "Sobrepeso"
        imc in 30.0..34.9 -> "Obesidad clase 1"
        imc in 35.0..39.9 -> "Obesidad clase 2"
        else -> "Obesidad clase 3"
    }
}

@Preview(showBackground = true)
@Composable
fun Previsualizacion() {
    AppNavegacion()
}