const express = require('express');
const mysql = require('mysql2');
const cors = require('cors');
const bodyParser = require('body-parser');

const app = express();
const PORT = 3000;

app.use(cors());
app.use(bodyParser.json());

app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);
    if (req.body && Object.keys(req.body).length > 0) {
        console.log('Body:', JSON.stringify(req.body, null, 2));
    }
    next();
});

// Config Base de datos
const db = mysql.createConnection({
    host: '127.0.0.1',
    user: 'gastos_user',
    password: 'TLLAMCFGYG',
    database: 'gastos_sync'
});

// Conectar DB
db.connect((err) => {
    if (err) {
        console.error('Error conectando a MariaDB:', err);
        return;
    }
    console.log('Conectado a MariaDB');
});

// Crear tablas si no existen
const createTables = () => {
    const createPersonasTable = `
        CREATE TABLE IF NOT EXISTS personas (
            id VARCHAR(255) PRIMARY KEY,
            nombre VARCHAR(255) NOT NULL,
            email VARCHAR(255),
            fecha_mod TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        )
    `;

    const createGastosTable = `
        CREATE TABLE IF NOT EXISTS gastos (
            id VARCHAR(255) PRIMARY KEY,
            descripcion VARCHAR(255) NOT NULL,
            monto DECIMAL(10,2) NOT NULL,
            pagado_por_id VARCHAR(255) NOT NULL,
            fecha BIGINT NOT NULL,
            categoria VARCHAR(255),
            division_igual BOOLEAN,
            fecha_mod TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (pagado_por_id) REFERENCES personas(id)
        )
    `;

    const createParticipantesTable = `
        CREATE TABLE IF NOT EXISTS participantes (
            gasto_id VARCHAR(255),
            persona_id VARCHAR(255),
            monto DECIMAL(10,2),
            fecha_mod TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (gasto_id, persona_id),
            FOREIGN KEY (gasto_id) REFERENCES gastos(id),
            FOREIGN KEY (persona_id) REFERENCES personas(id)
        )
    `;

    const createPagosTable = `
        CREATE TABLE IF NOT EXISTS pagos (
            id VARCHAR(255) PRIMARY KEY,
            deuda_original_id VARCHAR(255),
            pagador_id VARCHAR(255),
            receptor_id VARCHAR(255),
            monto DECIMAL(10,2),
            fecha BIGINT,
            nota TEXT,
            tipo VARCHAR(255),
            fecha_mod TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (pagador_id) REFERENCES personas(id),
            FOREIGN KEY (receptor_id) REFERENCES personas(id)
        )
    `;

    db.query(createPersonasTable, (err) => {
        if (err) console.error('Error creando tabla personas:', err);
    });

    db.query(createGastosTable, (err) => {
        if (err) console.error('Error creando tabla gastos:', err);
    });

    db.query(createParticipantesTable, (err) => {
        if (err) console.error('Error creando tabla participantes:', err);
    });

    db.query(createPagosTable, (err) => {
        if (err) console.error('Error creando tabla pagos:', err);
    });
};

createTables();

// Rutas de la API

// Obtener personas
app.get('/api/personas', (req, res) => {
    db.query('SELECT *, UNIX_TIMESTAMP(fecha_mod) as fecha_mod_timestamp FROM personas', (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json(results);
    });
});

// Obtener gastos
app.get('/api/gastos', (req, res) => {
    db.query('SELECT *, UNIX_TIMESTAMP(fecha_mod) as fecha_mod_timestamp FROM gastos', (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json(results);
    });
});

// Obtener participantes
app.get('/api/participantes', (req, res) => {
    db.query('SELECT *, UNIX_TIMESTAMP(fecha_mod) as fecha_mod_timestamp FROM participantes', (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json(results);
    });
});

// Obtener pagos
app.get('/api/pagos', (req, res) => {
    db.query('SELECT *, UNIX_TIMESTAMP(fecha_mod) as fecha_mod_timestamp FROM pagos', (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json(results);
    });
});

// Sincronizar personas
app.post('/api/sync/personas', (req, res) => {
    const personas = req.body;
    
    if (!Array.isArray(personas)) {
        res.status(400).json({ error: 'Se esperaba un array de personas' });
        return;
    }

    personas.forEach(persona => {
        const { id, nombre, email } = persona;
        const query = `
            INSERT INTO personas (id, nombre, email) 
            VALUES (?, ?, ?) 
            ON DUPLICATE KEY UPDATE 
            nombre = VALUES(nombre), 
            email = VALUES(email)
        `;
        
        db.query(query, [id, nombre, email], (err) => {
            if (err) console.error('Error sincronizando persona:', err);
        });
    });

    res.json({ message: 'Personas sincronizadas correctamente' });
});

// Sincronizar gastos
app.post('/api/sync/gastos', (req, res) => {
    const gastos = req.body;
    
    if (!Array.isArray(gastos)) {
        res.status(400).json({ error: 'Se esperaba un array de gastos' });
        return;
    }

    gastos.forEach(gasto => {
        const { id, descripcion, monto, pagado_por_id, fecha, categoria, division_igual } = gasto;
        const query = `
            INSERT INTO gastos (id, descripcion, monto, pagado_por_id, fecha, categoria, division_igual) 
            VALUES (?, ?, ?, ?, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE 
            descripcion = VALUES(descripcion), 
            monto = VALUES(monto),
            pagado_por_id = VALUES(pagado_por_id),
            fecha = VALUES(fecha),
            categoria = VALUES(categoria),
            division_igual = VALUES(division_igual)
        `;
        
        db.query(query, [id, descripcion, monto, pagado_por_id, fecha, categoria, division_igual], (err) => {
            if (err) console.error('Error sincronizando gasto:', err);
        });
    });

    res.json({ message: 'Gastos sincronizados correctamente' });
});

// Sincronizar participantes
app.post('/api/sync/participantes', (req, res) => {
    const participantes = req.body;
    
    if (!Array.isArray(participantes)) {
        res.status(400).json({ error: 'Se esperaba un array de participantes' });
        return;
    }

    participantes.forEach(participante => {
        const { gasto_id, persona_id, monto } = participante;
        const query = `
            INSERT INTO participantes (gasto_id, persona_id, monto) 
            VALUES (?, ?, ?) 
            ON DUPLICATE KEY UPDATE 
            monto = VALUES(monto)
        `;
        
        db.query(query, [gasto_id, persona_id, monto], (err) => {
            if (err) console.error('Error sincronizando participante:', err);
        });
    });

    res.json({ message: 'Participantes sincronizados correctamente' });
});

// Sincronizar pagos
app.post('/api/sync/pagos', (req, res) => {
    const pagos = req.body;
    
    if (!Array.isArray(pagos)) {
        res.status(400).json({ error: 'Se esperaba un array de pagos' });
        return;
    }

    pagos.forEach(pago => {
        const { id, deuda_original_id, pagador_id, receptor_id, monto, fecha, nota, tipo } = pago;
        const query = `
            INSERT INTO pagos (id, deuda_original_id, pagador_id, receptor_id, monto, fecha, nota, tipo) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE 
            deuda_original_id = VALUES(deuda_original_id),
            pagador_id = VALUES(pagador_id),
            receptor_id = VALUES(receptor_id),
            monto = VALUES(monto),
            fecha = VALUES(fecha),
            nota = VALUES(nota),
            tipo = VALUES(tipo)
        `;
        
        db.query(query, [id, deuda_original_id, pagador_id, receptor_id, monto, fecha, nota, tipo], (err) => {
            if (err) console.error('Error sincronizando pago:', err);
        });
    });

    res.json({ message: 'Pagos sincronizados correctamente' });
});


app.listen(PORT, '0.0.0.0', () => {
    console.log(`Servidor en http://192.168.98.82:${PORT}`);
    console.log(`IP para Android: http://192.168.98.82:${PORT}`);
});

