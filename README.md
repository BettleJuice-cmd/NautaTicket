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

## Integrantes

- Andy Steven Díaz Montenegro (Lider)
- Alexa Sofía Loaisiga Torrez
- Adolfo Carlos Ramírez Aráuz
- Carlos Fernando Torres González

## Información académica

**Universidad:** Universidad Americana (UAM)  
**Carrera:** Ingeniería en Sistemas de Información  
**Asignatura:** Programación de Aplicaciones de Escritorio (PAE)  
**Proyecto:** Proyecto final

---

**NautaTicket — Conectando problemas con soluciones.**
