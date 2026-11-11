
BEGIN;

CREATE TABLE roles (
                       id_rol INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       nombre VARCHAR(30) NOT NULL UNIQUE
);

INSERT INTO roles (nombre)
VALUES ('EMPLEADO'), ('TECNICO'), ('ADMIN');

ALTER TABLE usuarios
    ADD COLUMN id_rol INTEGER;

UPDATE usuarios u
SET id_rol = r.id_rol
    FROM roles r
WHERE UPPER(TRIM(u.rol)) = r.nombre;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuarios WHERE id_rol IS NULL) THEN
        RAISE EXCEPTION 'Existen usuarios sin rol reconocido';
END IF;
END $$;

ALTER TABLE usuarios
    ALTER COLUMN id_rol SET NOT NULL;

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_roles
        FOREIGN KEY (id_rol) REFERENCES roles(id_rol);

ALTER TABLE usuarios
    ALTER COLUMN rol DROP NOT NULL;

ALTER TABLE usuarios
    ALTER COLUMN rol DROP DEFAULT;

CREATE TABLE estados_ticket (
                                id_estado INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                nombre VARCHAR(30) NOT NULL UNIQUE
);

INSERT INTO estados_ticket (nombre)
VALUES ('PENDIENTE'), ('EN_PROCESO'), ('RESUELTO'), ('CERRADO');

ALTER TABLE tickets
    ADD COLUMN id_estado INTEGER;

UPDATE tickets t
SET id_estado = e.id_estado
    FROM estados_ticket e
WHERE UPPER(TRIM(t.estado)) = e.nombre;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM tickets WHERE id_estado IS NULL) THEN
        RAISE EXCEPTION 'Existen tickets sin estado reconocido';
END IF;
END $$;

ALTER TABLE tickets
    ALTER COLUMN id_estado SET NOT NULL;

ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_estados
        FOREIGN KEY (id_estado) REFERENCES estados_ticket(id_estado);

ALTER TABLE tickets
    ALTER COLUMN estado DROP NOT NULL;

ALTER TABLE tickets
    ALTER COLUMN estado DROP DEFAULT;

CREATE TABLE asignaciones_ticket (
                                     id_asignacion INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     id_ticket INTEGER NOT NULL
                                         REFERENCES tickets(id_ticket),
                                     id_tecnico INTEGER NOT NULL
                                         REFERENCES usuarios(id_usuario),
                                     fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     fecha_fin TIMESTAMP
);

INSERT INTO asignaciones_ticket
(id_ticket, id_tecnico, fecha_asignacion)
SELECT
    id_ticket,
    id_tecnico,
    COALESCE(fecha_asignacion, CURRENT_TIMESTAMP)
FROM tickets
WHERE id_tecnico IS NOT NULL;

COMMIT;
