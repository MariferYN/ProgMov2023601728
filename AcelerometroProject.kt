package com.example.acelerometro

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lastAcceleration = 9.8f // Gravedad terrestre
    private var lastTimestamp = 0L
    private var golpeDetectado = mutableStateOf(false) // Estado observable para golpeDetectado
    private var mediaPlayer: MediaPlayer? = null
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var saqueJob: Job? = null
    private var golpeJob: Job? = null
    private var lastGolpeTime = 0L // Tiempo del último golpe detectado
    private lateinit var vibrator: Vibrator // Servicio de vibración
    private var golpeCount = 0 // Contador de golpes
    private var mostrarPuntaje = mutableStateOf(false) // Estado para mostrar la imagen de puntaje

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SensorManager::class.java)
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        setContent {
            UIPrincipal(golpeDetectado.value, mostrarPuntaje.value)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val acceleration = sqrt(
                (it.values[0] * it.values[0] +
                        it.values[1] * it.values[1] +
                        it.values[2] * it.values[2]).toDouble()
            ).toFloat()

            val delta = acceleration - lastAcceleration
            lastAcceleration = acceleration
            val currentTime = System.nanoTime()

            if (currentTime - lastGolpeTime < 500_000_000) return // 500 ms entre golpes

            when {
                delta > 15 -> {
                    registrarGolpe(R.raw.sonido3)
                }
                delta > 8 -> {
                    registrarGolpe(R.raw.sonido1)
                }
                golpeDetectado.value && delta < 1 -> {
                    golpeDetectado.value = false
                }
            }
        }
    }

    private fun registrarGolpe(sonido: Int) {
        golpeDetectado.value = true
        lastGolpeTime = System.nanoTime()
        cancelarSaque()
        reproducirGolpe(sonido)
        vibrar()
        golpeCount++

        if (golpeCount >= 6) {
            mostrarImagenPuntaje()
            golpeCount = 0
        }
    }

    private fun mostrarImagenPuntaje() {
        mostrarPuntaje.value = true
        reproducirSonido(R.raw.aplausos)
        scope.launch {
            delay(8000)
            mostrarPuntaje.value = false
        }
    }

    private fun vibrar() {
        if (vibrator.hasVibrator()) {
            val vibrationEffect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        }
    }

    private fun reproducirGolpe(sonido: Int) {
        if (mediaPlayer?.isPlaying == true) return
        golpeJob?.cancel()
        golpeJob = scope.launch {
            mutex.withLock {
                reproducirSonido(sonido)
            }
        }
    }

    private fun reproducirSonido(sonido: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, sonido).apply {
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelarSaque() {
        saqueJob?.cancel()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        scope.cancel()
        mediaPlayer?.release()
    }
}

@Composable
fun UIPrincipal(golpeDetectado: Boolean, mostrarPuntaje: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val imagen = when {
            mostrarPuntaje -> painterResource(id = R.drawable.puntaje)
            golpeDetectado -> painterResource(id = R.drawable.golpe)
            else -> painterResource(id = R.drawable.raqueta)
        }

        Image(
            painter = imagen,
            contentDescription = when {
                mostrarPuntaje -> "Puntaje"
                golpeDetectado -> "Golpe detectado"
                else -> "Raqueta"
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    UIPrincipal(golpeDetectado = false, mostrarPuntaje = false)
}