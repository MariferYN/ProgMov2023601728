# Gestor de Gastos Compartidos

Una aplicación móvil Android para gestionar gastos compartidos entre grupos de personas con sincronización offline.

## Características

- ✅ Gestión de personas y gastos compartidos
- ✅ Cálculo automático de deudas
- ✅ Sincronización offline entre dispositivos
- ✅ Interfaz con Jetpack Compose
- ✅ Base de datos local SQLite
- ✅ Servidor Node.js para sincronización

## Stack Tecnológico

**Android:**
- Kotlin
- Jetpack Compose
- SQLite (DatabaseHelper)
- Ktor Client (HTTP requests)
- Coroutines

**Servidor:**
- Node.js
- Express.js
- MariaDB/MySQL
- CORS

## 📊 Esquema de Base de Datos

### Tabla `personas`
```sql
CREATE TABLE personas (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    email TEXT,
    fecha_mod INTEGER DEFAULT (strftime('%s', 'now'))
);
```

### Tabla `gastos`
```sql
CREATE TABLE gastos (
    id TEXT PRIMARY KEY,
    descripcion TEXT NOT NULL,
    monto REAL NOT NULL,
    pagado_por_id TEXT NOT NULL,
    fecha INTEGER NOT NULL,
    categoria TEXT,
    division_igual INTEGER,
    fecha_mod INTEGER DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY(pagado_por_id) REFERENCES personas(id)
);
```

### Tabla `participantes`
```sql
CREATE TABLE participantes (
    gasto_id TEXT,
    persona_id TEXT,
    monto REAL,
    fecha_mod INTEGER DEFAULT (strftime('%s', 'now')),
    PRIMARY KEY(gasto_id, persona_id),
    FOREIGN KEY(gasto_id) REFERENCES gastos(id),
    FOREIGN KEY(persona_id) REFERENCES personas(id)
);
```

### Tabla `pagos`
```sql
CREATE TABLE pagos (
    id TEXT PRIMARY KEY,
    deuda_original_id TEXT,
    pagador_id TEXT,
    receptor_id TEXT,
    monto REAL,
    fecha INTEGER,
    nota TEXT,
    tipo TEXT,
    fecha_mod INTEGER DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY(pagador_id) REFERENCES personas(id),
    FOREIGN KEY(receptor_id) REFERENCES personas(id)
);
```

## 🔄 Sincronización

La aplicación implementa sincronización bidireccional entre dispositivos usando:

1. **Servidor local** (Node.js + MariaDB)
2. **Comparación por timestamps** (`fecha_mod`)

### Endpoints de la API

```
GET /api/personas          # Obtener todas las personas
GET /api/gastos            # Obtener todos los gastos
GET /api/participantes     # Obtener participantes
GET /api/pagos             # Obtener pagos

POST /api/sync/personas    # Sincronizar personas
POST /api/sync/gastos      # Sincronizar gastos
POST /api/sync/participantes  # Sincronizar participantes
POST /api/sync/pagos       # Sincronizar pagos
```

## ⚙️ Configuración

### Servidor

1. **Instalar dependencias:**
```bash
npm install express mysql2 cors body-parser
```

2. **Configurar MariaDB:**
```sql
CREATE DATABASE gastos_sync;
CREATE USER 'gastos_user'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON gastos_sync.* TO 'gastos_user'@'localhost';
```

3. **Ejecutar servidor:**
```bash
node server.js
```

### Android

1. **Configurar IP del servidor en `SyncManager.kt`:**
```kotlin
private val serverUrl = "http://TU_IP_LOCAL:3000/api"
```

2. **Permisos en `AndroidManifest.xml`:**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

3. **Permitir HTTP :**
```xml
<application android:usesCleartextTraffic="true">
```

## 🚦 Uso

1. **Conectar dispositivos**
2. **Ejecutar servidor**
3. **Instalar app** en dispositivos Android
4. **Agregar personas y gastos** en cualquier dispositivo
5. **Presionar sincronizar** para compartir datos
