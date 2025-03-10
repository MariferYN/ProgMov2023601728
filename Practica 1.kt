import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Scanner

fun main() {
    val scanner = Scanner(System.`in`)
    var opcion: Int

    do {
        println("\nMenu de opciones:")
        println("1. Suma de 3 numeros")
        println("2. Ingresar nombre completo")
        println("3. Fecha de nacimientos")
        println("4. Salir")
        print("Seleccione una opción: ")
        opcion = scanner.nextInt()
        scanner.nextLine()

        when (opcion) {
            1 -> sumarTresNumeros(scanner)
            2 -> ingresarNombre(scanner)
            3 -> calcularTiempoVivido(scanner)
            4 -> println("Saliendo del programa...")
            else -> println("Opcion no valida, intente de nuevo.")
        }
    } while (opcion != 4)
}

fun sumarTresNumeros(scanner: Scanner) {
    print("Ingrese el primer número: ")
    val num1 = scanner.nextInt()
    print("Ingrese el segundo número: ")
    val num2 = scanner.nextInt()
    print("Ingrese el tercer número: ")
    val num3 = scanner.nextInt()
    val suma = num1 + num2 + num3
    println("La suma de los tres números es: $suma")
}

fun ingresarNombre(scanner: Scanner) {
    print("Ingrese su nombre completo: ")
    val nombre = scanner.nextLine()
    println("Nombre ingresado: $nombre")
}

fun calcularTiempoVivido(scanner: Scanner) {
    print("Ingrese su fecha de nacimiento (YYYY-MM-DD): ")
    val fechaNacimiento = LocalDate.parse(scanner.nextLine())
    val fechaActual = LocalDate.now()
    val periodo = Period.between(fechaNacimiento, fechaActual)

    val diasVividos = ChronoUnit.DAYS.between(fechaNacimiento, fechaActual)
    val semanasVividas = diasVividos / 7
    val mesesVividos = periodo.years * 12 + periodo.months
    val horasVividas = diasVividos * 24
    val minutosVividos = horasVividas * 60
    val segundosVividos = minutosVividos * 60

    println("Tiempo vivido:")
    println("Meses: $mesesVividos")
    println("Días: $diasVividos")
    println("Semanas: $semanasVividas")
    println("Horas: $horasVividas")
    println("Minutos: $minutosVividos")
    println("Segundos: $segundosVividos")
}
