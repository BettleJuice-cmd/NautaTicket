# NautaTicket

### Sistema de Gestión de Tickets de Soporte TI

## Descripción del proyecto

**NautaTicket** es una aplicación de escritorio diseñada para gestionar las solicitudes de soporte técnico de los diferentes departamentos de una empresa.

Su propósito es facilitar la comunicación entre los empleados y el departamento de Tecnologías de la Información (TI), permitiendo reportar problemas relacionados con equipos informáticos, programas, redes y otros recursos tecnológicos que requieran asistencia especializada.

Cada solicitud se registra mediante un **ticket**, que permite al personal de TI organizar, asignar, atender y dar seguimiento a las incidencias según su orden de llegada, hasta que sean resueltas.

## Objetivo

Desarrollar un sistema que centralice y organice las solicitudes de soporte técnico de una empresa, facilitando la atención de incidencias por parte de ingenieros en sistemas y personal especializado en TI.

## Funcionalidades principales

- Registro de usuarios pertenecientes a distintos departamentos.
- Creación de tickets para reportar problemas tecnológicos.
- Organización de solicitudes según su orden de llegada.
- Asignación de tickets al personal de soporte TI.
- Seguimiento del estado de cada incidencia.
- Registro de soluciones e historial de tickets.
- Estadísticas en el Dashboard y consulta de estadísticas mediante la API REST.
- Generación de reportes PDF con filtros de fecha de creación y estado actual.

## Tecnologías y proyectos

- El **Desktop (`pom.xml`)** usa Java 21, JavaFX 21 con vistas FXML, Hibernate ORM 6.6, JPA, Lombok, PostgreSQL, Jackson para el consumo REST y JasperReports 7.0.8 para los PDF.
- La **API (`nautaticket-api/pom.xml`)** es un proyecto Maven independiente con Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Security, Spring JDBC y PostgreSQL. Sus endpoints actuales son de consulta.
- **PostgreSQL** almacena el esquema inicial y las migraciones SQL de `database/`. El Desktop usa Hibernate con validación del esquema y la API consulta el mismo esquema mediante JDBC.

## Preparar PostgreSQL

Se necesita una instalación de PostgreSQL y una cuenta con permisos para crear la base y aplicar los scripts. Desde la raíz del repositorio, aplica los archivos en este orden. Sustituye `USUARIO_ADMIN_PG` por la cuenta administrativa local. `01_create_database.sql` crea `nautaticket_db`; los demás scripts se ejecutan dentro de esa base.

```powershell
psql -X -v ON_ERROR_STOP=1 -U USUARIO_ADMIN_PG -d postgres -f database/01_create_database.sql
psql -X -v ON_ERROR_STOP=1 -U USUARIO_ADMIN_PG -d nautaticket_db -f database/02_schema.sql
psql -X -v ON_ERROR_STOP=1 -U USUARIO_ADMIN_PG -d nautaticket_db -f database/03_seed.sql
psql -X -v ON_ERROR_STOP=1 -U USUARIO_ADMIN_PG -d nautaticket_db -f database/04_migracion_hibernate.sql
psql -X -v ON_ERROR_STOP=1 -U USUARIO_ADMIN_PG -d nautaticket_db -f database/05_asignacion_activa_unica.sql
```

El script 03 carga departamentos y categorías de ejemplo. La migración 04 incorpora roles, estados de ticket y asignaciones. La API tiene desactivada la inicialización automática del esquema. Antes de aplicar el script 05 en una base con datos, sigue la revisión descrita en la sección siguiente. Los scripts no crean usuarios de aplicación. Para preparar el primer administrador existe `InicializarAdminLocal`, que requiere `ADMIN_EMAIL` y `ADMIN_PASSWORD` y debe ejecutarse explícitamente después de preparar el esquema.

## Variables de entorno

Configura los valores en el entorno de cada proceso antes de iniciarlo.

| Variable | Uso |
|---|---|
| `DB_URL` | URL JDBC de PostgreSQL. En el Desktop, si se omite, se usa la base local `nautaticket_db`. La API la toma de esta variable. |
| `DB_USER` | Usuario de PostgreSQL. En el Desktop, si se omite, se usa `postgres`. La API la toma de esta variable. |
| `DB_PASSWORD` | Contraseña de PostgreSQL. Obligatoria para iniciar el acceso a datos del Desktop y para la configuración JDBC de la API. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Solo para ejecutar `InicializarAdminLocal` al crear el primer administrador. La contraseña debe tener al menos 12 caracteres. |
| `NAUTATICKET_TEST_DB_URL`, `NAUTATICKET_TEST_DB_USER`, `NAUTATICKET_TEST_DB_PASSWORD` | Solo para las pruebas PostgreSQL del Desktop. La URL debe señalar una base desechable llamada `nautaticket_test`. |

## Iniciar cada aplicación

Ejecuta cada proyecto por separado con Java 21 y sus variables de entorno configuradas. El Desktop necesita el esquema preparado. Para usar el botón **Consultar API REST**, inicia también la API. En Windows:

```powershell
# Terminal 1, desde la raíz del repositorio
.\mvnw.cmd javafx:run

# Terminal 2, desde la raíz del repositorio
cd nautaticket-api
.\mvnw.cmd spring-boot:run
```

En sistemas con shell Unix, usa `./mvnw` en lugar de `.\mvnw.cmd`. La API escucha en el puerto `8081` y su configuración local fija `server.address=127.0.0.1`. El Desktop consulta `http://localhost:8081/api/v1/estadisticas` desde el botón del Dashboard. Este cliente utiliza autenticación HTTP Basic con las credenciales que se introducen en el diálogo de consulta.

## API REST y acceso

La API utiliza las cuentas activas de `usuarios` y sus roles en PostgreSQL. Las rutas disponibles son de solo lectura:

| Método y ruta | Acceso | Resultado |
|---|---|---|
| `GET /api/v1/diagnostico/db` | `ADMIN` | Estado de la conexión y nombre de la base. |
| `GET /api/v1/estadisticas` | `ADMIN` | Totales globales por estado, incluidos los estados con cero tickets. |
| `GET /api/v1/departamentos` | Cualquier usuario autenticado | Departamentos registrados. |
| `GET /api/v1/tickets` | `ADMIN`, `TECNICO`, `EMPLEADO` | `ADMIN` ve todos; `TECNICO`, los asignados a su cuenta; `EMPLEADO`, los solicitados por su cuenta. |

Las estadísticas REST son globales y no aceptan filtros de fecha. Los filtros de fecha de creación y estado del Dashboard local y del reporte PDF siguen siendo independientes de ese endpoint. La API está restringida a la interfaz local configurada.

## Atención FIFO y reportes

La toma del siguiente ticket ordena los pendientes por `fecha_creacion` e `id_ticket`. La operación usa una transacción Hibernate, un bloqueo consultivo transaccional de PostgreSQL y un bloqueo de fila para serializar las tomas concurrentes. En la misma transacción registra la asignación y el historial. La migración 05 limita a una asignación activa por ticket. Al resolverlo, la operación exige cerrar exactamente una asignación activa y registra la transición en el historial. La espera de la toma se ejecuta fuera del hilo JavaFX.

`ADMIN` puede generar un PDF general con los filtros existentes de fecha de creación y estado actual. `TECNICO` solo puede incluir tickets cuyo historial registre que ese usuario inició la atención mediante la transición `PENDIENTE` → `EN_PROCESO`, aunque el ticket esté ahora resuelto o cerrado. Una mera asignación no lo incluye. `EMPLEADO` no tiene acceso al Dashboard ni a la generación de reportes. `ReporteDAO` comprueba el usuario activo y su rol al consultar los datos. El botón **Consultar API REST** del Dashboard permanece reservado a `ADMIN`.

## Migración 05 y verificación

Aplica `database/05_asignacion_activa_unica.sql` después de la migración 04.
El script instala un índice único sobre `id_ticket` para las asignaciones con
`fecha_fin IS NULL`. Las asignaciones cerradas se conservan. Programa la ejecución
durante una ventana de mantenimiento, porque la migración bloquea escrituras
sobre `tickets` y `asignaciones_ticket` hasta el commit.

En una base con datos, revisa estas consultas de solo lectura antes de aplicar la migración.

```sql
SELECT id_ticket, array_agg(id_asignacion ORDER BY id_asignacion) AS asignaciones
FROM asignaciones_ticket
WHERE fecha_fin IS NULL
GROUP BY id_ticket
HAVING COUNT(*) > 1;

SELECT a.id_ticket, a.id_asignacion, a.id_tecnico, t.id_tecnico AS tecnico_ticket,
       a.fecha_asignacion, e.nombre AS estado, t.fecha_resolucion, t.fecha_cierre
FROM asignaciones_ticket a
JOIN tickets t ON t.id_ticket = a.id_ticket
JOIN estados_ticket e ON e.id_estado = t.id_estado
WHERE a.fecha_fin IS NULL AND e.nombre IN ('RESUELTO', 'CERRADO')
ORDER BY a.id_ticket, a.id_asignacion;
```

La migración aborta por duplicados activos. Para un ticket finalizado con una
asignación abierta, completa `fecha_fin` con la fecha de resolución registrada,
o con la de cierre si el ticket está cerrado y carece de fecha de resolución.
Exige que coincida el técnico y que la fecha sea igual o posterior a la
asignación. Cualquier caso restante aborta la transacción completa. Revisa la
evidencia del ticket antes de corregir esos registros y volver a ejecutar la
migración. Usa `psql -v ON_ERROR_STOP=1` al aplicar el archivo.

Las pruebas de integración requieren una base PostgreSQL desechable llamada
`nautaticket_test` y una cuenta con permiso para crear esquemas en esa base.
Configura `NAUTATICKET_TEST_DB_URL`, `NAUTATICKET_TEST_DB_USER` y
`NAUTATICKET_TEST_DB_PASSWORD` y ejecuta `./mvnw test` o `mvnw.cmd test` en Windows.
Cada prueba crea y retira su propio esquema. Sin la URL de prueba, JUnit omite
estos casos.

## Compilación y pruebas

Ejecuta los comandos siguientes desde la raíz del repositorio con Java 21. La suite PostgreSQL del Desktop crea y elimina esquemas únicamente dentro de `nautaticket_test`; verifica la URL antes de ejecutarla. El proyecto API tiene su propia prueba de arranque de contexto, que requiere la configuración JDBC del entorno.

```powershell
# Desktop
.\mvnw.cmd clean compile
.\mvnw.cmd test

# API, desde la raíz del repositorio
cd nautaticket-api
.\mvnw.cmd clean compile
.\mvnw.cmd test
```

Las pruebas del Desktop cubren perfiles, FIFO concurrente, transiciones e historial, la migración 05 y el acceso a reportes. Las de la API comprueban el arranque del contexto Spring. El resultado concreto de cada ejecución depende de la base y las variables configuradas en ese entorno.

## Integrantes

El código y el historial de cambios muestran estas áreas de trabajo compartidas.

- **Andy Steven Díaz Montenegro**, líder, trabajó en bienvenida, selección de perfil y administración de usuarios.
- **Alexa Sofía Loaisiga Torrez** trabajó en modelos y DAO, y en componentes de la interfaz del Dashboard.
- **Adolfo Carlos Ramírez Aráuz** trabajó en atención FIFO, integración REST entre el Desktop y la API, y control de acceso a reportes.
- **Carlos Fernando Torres González** trabajó en historial de tickets e integración de JasperReports para generar PDF.

## Información académica

**Universidad:** Universidad Americana (UAM)  
**Carrera:** Ingeniería en Sistemas de Información  
**Asignatura:** Programación de Aplicaciones de Escritorio (PAE)  
**Proyecto:** Proyecto final

---

**NautaTicket — Conectando problemas con soluciones.**
