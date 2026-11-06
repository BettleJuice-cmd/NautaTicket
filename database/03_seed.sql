
-- Departamentos de ejemplo

INSERT INTO departamentos (nombre, descripcion)
VALUES
    ('Contabilidad', 'Departamento contable'),
    ('Recursos Humanos', 'Gestión del personal'),
    ('Ventas', 'Departamento comercial'),
    ('Administración', 'Gestión administrativa'),
    ('Tecnologías de la Información', 'Soporte técnico')
    ON CONFLICT (nombre) DO NOTHING;

-- Categorías de incidencias

INSERT INTO categorias (nombre, descripcion)
VALUES
    ('Hardware', 'Problemas con equipos físicos'),
    ('Software', 'Problemas con programas y aplicaciones'),
    ('Redes', 'Problemas de conexión y acceso a la red'),
    ('Impresoras', 'Problemas de impresión'),
    ('Accesos', 'Problemas con cuentas y permisos'),
    ('Otros', 'Otras solicitudes de soporte')
    ON CONFLICT (nombre) DO NOTHING;
