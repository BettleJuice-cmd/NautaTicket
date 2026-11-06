-- NautaTicket - Esquema inicial PostgreSQL

CREATE TABLE IF NOT EXISTS departamentos (
                                             id_departamento INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                             nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS usuarios (
                                        id_usuario INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        id_departamento INTEGER,
                                        nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_usuario_departamento
    FOREIGN KEY (id_departamento)
    REFERENCES departamentos(id_departamento),

    CONSTRAINT chk_usuario_rol
    CHECK (rol IN ('EMPLEADO', 'TECNICO', 'ADMIN'))
    );

CREATE TABLE IF NOT EXISTS categorias (
                                          id_categoria INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                          nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activa BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS tickets (
                                       id_ticket INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                       id_solicitante INTEGER NOT NULL,
                                       id_categoria INTEGER NOT NULL,
                                       id_tecnico INTEGER,
                                       titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_asignacion TIMESTAMP,
    fecha_resolucion TIMESTAMP,
    fecha_cierre TIMESTAMP,
    solucion TEXT,

    CONSTRAINT fk_ticket_solicitante
    FOREIGN KEY (id_solicitante)
    REFERENCES usuarios(id_usuario),

    CONSTRAINT fk_ticket_categoria
    FOREIGN KEY (id_categoria)
    REFERENCES categorias(id_categoria),

    CONSTRAINT fk_ticket_tecnico
    FOREIGN KEY (id_tecnico)
    REFERENCES usuarios(id_usuario),

    CONSTRAINT chk_ticket_estado
    CHECK (estado IN (
           'PENDIENTE',
           'EN_PROCESO',
           'RESUELTO',
           'CERRADO'
                     )),

    CONSTRAINT chk_ticket_descripcion
    CHECK (LENGTH(TRIM(descripcion)) > 0),

    CONSTRAINT chk_ticket_titulo
    CHECK (LENGTH(TRIM(titulo)) > 0)
    );

CREATE TABLE IF NOT EXISTS historial_tickets (
                                                 id_historial INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                 id_ticket INTEGER NOT NULL,
                                                 id_usuario INTEGER NOT NULL,
                                                 estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20),
    comentario TEXT,
    fecha_cambio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historial_ticket
    FOREIGN KEY (id_ticket)
    REFERENCES tickets(id_ticket),

    CONSTRAINT fk_historial_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES usuarios(id_usuario),

    CONSTRAINT chk_historial_estado
    CHECK (
    (estado_anterior IS NULL OR estado_anterior IN (
     'PENDIENTE', 'EN_PROCESO', 'RESUELTO', 'CERRADO'
                                                   ))
    AND
(estado_nuevo IS NULL OR estado_nuevo IN (
 'PENDIENTE', 'EN_PROCESO', 'RESUELTO', 'CERRADO'
                                         ))
    )
    );

-- Índices para consultas frecuentes

CREATE INDEX IF NOT EXISTS idx_ticket_cola
    ON tickets(estado, fecha_creacion, id_ticket);

CREATE INDEX IF NOT EXISTS idx_ticket_solicitante
    ON tickets(id_solicitante);

CREATE INDEX IF NOT EXISTS idx_ticket_tecnico
    ON tickets(id_tecnico);

CREATE INDEX IF NOT EXISTS idx_historial_ticket
    ON historial_tickets(id_ticket, fecha_cambio);