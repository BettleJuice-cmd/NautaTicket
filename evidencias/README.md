# Evidencias académicas de NautaTicket

Las cinco imágenes son composiciones de capturas auténticas del código y de resultados Surefire abiertos en IntelliJ IDEA el 17 de noviembre de 2026. Las pruebas que escribieron datos utilizaron exclusivamente PostgreSQL temporal, con la base `nautaticket_test`. El clúster temporal fue detenido al finalizar.

| Imagen | Qué acredita | Fuente verificable |
|---|---|---|
| [01-entidades-hibernate.png](01-entidades-hibernate.png) | Anotaciones JPA de `Ticket`, su clave generada, las ocho entradas `<mapping>` y `hbm2ddl.auto=validate`. | [`Ticket.java`](../src/main/java/ni/edu/uam/nautaticket/model/Ticket.java), [`hibernate.cfg.xml`](../src/main/resources/hibernate.cfg.xml) |
| [02-relaciones-jpa.png](02-relaciones-jpa.png) | `Ticket.asignaciones` con `@OneToMany(mappedBy = "ticket")`, `AsignacionTicket.ticket` con `@ManyToOne` y `@JoinColumn`, y los métodos de ayuda de ambos lados. | [`Ticket.java`](../src/main/java/ni/edu/uam/nautaticket/model/Ticket.java), [`AsignacionTicket.java`](../src/main/java/ni/edu/uam/nautaticket/model/AsignacionTicket.java) |
| [03-crud-persistencia.png](03-crud-persistencia.png) | Prueba de creación, consulta, actualización y eliminación con `TicketDAO`; tras reconstruir el `SessionFactory` comprueba que la actualización persistió. El reporte Surefire muestra 18 pruebas PostgreSQL aprobadas, sin fallos ni omisiones. | [`AtencionPostgresTest.java`](../src/test/java/ni/edu/uam/nautaticket/util/AtencionPostgresTest.java), `target/surefire-reports/ni.edu.uam.nautaticket.util.AtencionPostgresTest.txt` (generado localmente) |
| [04-hql-transacciones.png](04-hql-transacciones.png) | HQL con `JOIN FETCH`, parámetros nombrados y agregación `COUNT`/`GROUP BY`; transacción de atención con bloqueo de cola y verificación de capacidad. | [`HistorialTicketDAO.java`](../src/main/java/ni/edu/uam/nautaticket/dao/HistorialTicketDAO.java), [`AtencionService.java`](../src/main/java/ni/edu/uam/nautaticket/service/AtencionService.java) |
| [05-integracion-nautaticket.png](05-integracion-nautaticket.png) | Pruebas de integración de creación y consulta de tickets, cola FIFO, límite de tres, inicio y resolución; el reporte Surefire acredita la ejecución de la clase. **Es evidencia del flujo probado, no una captura de la interfaz JavaFX.** | [`AtencionPostgresTest.java`](../src/test/java/ni/edu/uam/nautaticket/util/AtencionPostgresTest.java), reporte Surefire indicado arriba |

## Verificación reproducible

Con Java 21 y un PostgreSQL desechable que contenga la base `nautaticket_test`, define `NAUTATICKET_TEST_DB_URL`, `NAUTATICKET_TEST_DB_USER` y `NAUTATICKET_TEST_DB_PASSWORD` para **esa instancia**, y ejecuta `./mvnw clean test` (Windows: `.\mvnw.cmd clean test`). Las pruebas crean y eliminan esquemas de trabajo solo en esa base. La ejecución de esta entrega registró **29 pruebas Desktop aprobadas, 0 fallos, 0 errores y 0 omisiones**; 18 pertenecen a `AtencionPostgresTest`. El reporte Surefire se regenera localmente y está ignorado por Git.

## Captura visual pendiente

Para acreditar la **ventana de la aplicación** además de la integración automatizada, falta una captura manual auténtica de JavaFX con cuentas y datos sintéticos en PostgreSQL desechable. Debe mostrar la creación de un ticket por EMPLEADO y, tras iniciar sesión como TECNICO, el ticket en la cola de Atención y la capacidad `X/3`. Puede adjuntarse como evidencia complementaria; no se debe presentar [05-integracion-nautaticket.png](05-integracion-nautaticket.png) como si mostrara esa interfaz. Verifica que la imagen no incluya correos, contraseñas ni datos reales.
