
package ni.edu.uam.nautaticket.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.HtmlResourceHandler;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;

import ni.edu.uam.nautaticket.dao.ReporteDAO;
import ni.edu.uam.nautaticket.dto.ReporteTicketDTO;
import ni.edu.uam.nautaticket.model.HistorialTicket;

import org.hibernate.SessionFactory;
import org.hibernate.Session;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.io.StringWriter;

public class ReporteService {

    private static final String PLANTILLA =
            "/reports/reporte_tickets.jrxml";

    private final ReporteDAO reporteDAO;
    private final SessionFactory sessionFactory;
    private final int usuarioId;

    public ReporteService(SessionFactory sessionFactory, int usuarioId) {
        if (usuarioId <= 0) {
            throw new IllegalArgumentException("El usuario del reporte no es válido.");
        }
        this.reporteDAO = new ReporteDAO(
                Objects.requireNonNull(sessionFactory)
        );
        this.sessionFactory = sessionFactory;
        this.usuarioId = usuarioId;
    }

    public Path generarReporte(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            Path destino) {
        if (destino == null) {
            throw new IllegalArgumentException("Debe seleccionar una ubicación para el PDF.");
        }
        return guardarReporte(prepararReporte(fechaDesde, fechaHasta, estado), destino);
    }

    public JasperPrint prepararReporte(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado) {
        return prepararReporte(fechaDesde, fechaHasta, estado, "GENERAL");
    }

    public JasperPrint prepararReporte(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            String tipo) {

        if (!"GENERAL".equals(tipo) && !"TICKETS".equals(tipo)
                && !"GRAFICOS".equals(tipo)) {
            throw new IllegalArgumentException("El tipo de reporte no está disponible.");
        }

        if (fechaDesde != null && fechaHasta != null
                && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final."
            );
        }

        String estadoFiltro = normalizarEstado(estado);

        if ("GRAFICOS".equals(tipo)) {
            return prepararEstadisticas(fechaDesde, fechaHasta, estadoFiltro);
        }
        if ("GENERAL".equals(tipo)) {
            return prepararGeneral(fechaDesde, fechaHasta, estadoFiltro);
        }

        List<ReporteTicketDTO> tickets =
                reporteDAO.obtenerTickets(
                        fechaDesde,
                        fechaHasta,
                        estadoFiltro,
                        usuarioId
                );

        Map<String, Long> totales = tickets.stream()
                .collect(Collectors.groupingBy(
                        ReporteTicketDTO::getEstado,
                        Collectors.counting()
                ));

        Map<String, Object> parametros = new HashMap<>();

        parametros.put("FECHA_DESDE", fechaDesde);
        parametros.put("FECHA_HASTA", fechaHasta);
        parametros.put(
                "ESTADO_FILTRO",
                estadoFiltro == null ? "TODOS" : estadoFiltro
        );
        parametros.put(
                "FECHA_GENERACION",
                LocalDateTime.now()
        );
        parametros.put("TOTAL_TICKETS", tickets.size());
        parametros.put("TOTALES_ESTADO", totales);
        parametros.put("SIN_RESULTADOS", tickets.isEmpty());
        parametros.put("TITULO_REPORTE", "Informe de tickets");
        parametros.put("REPORT_LOCALE", Locale.forLanguageTag("es-NI"));

        try (InputStream plantilla =
                     ReporteService.class.getResourceAsStream(
                             PLANTILLA
                     )) {

            if (plantilla == null) {
                throw new IllegalStateException(
                        "No se encontró la plantilla: " + PLANTILLA
                );
            }

            JasperReport reporte =
                    JasperCompileManager.compileReport(plantilla);

            JRBeanCollectionDataSource datos =
                    new JRBeanCollectionDataSource(tickets);

            JasperPrint documento =
                    JasperFillManager.fillReport(
                            reporte,
                            parametros,
                            datos
                    );

            return documento;

        } catch (JRException | IOException e) {
            throw new IllegalStateException(
                    "No se pudo preparar el reporte.", e
            );
        }
    }

    private JasperPrint prepararGeneral(
            LocalDate fechaDesde, LocalDate fechaHasta, String estadoFiltro) {
        List<ReporteTicketDTO> tickets = reporteDAO.obtenerTickets(
                fechaDesde, fechaHasta, estadoFiltro, usuarioId);
        Map<String, Long> estados = tickets.stream().collect(Collectors.groupingBy(
                ReporteTicketDTO::getEstado, Collectors.counting()));
        Map<String, Long> categorias = tickets.stream().collect(Collectors.groupingBy(
                ticket -> ticket.getCategoria() == null || ticket.getCategoria().isBlank()
                        ? "Sin categoría" : ticket.getCategoria(), Collectors.counting()));
        Map.Entry<String, Long> principal = categorias.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .orElse(null);

        boolean administrador = esAdministrador();
        int movimientos = 0;
        int cambiosEstado = 0;
        int cambiosTecnico = 0;
        if (administrador) {
            List<HistorialTicket> historial = new HistorialService(sessionFactory, usuarioId)
                    .buscar(null, fechaDesde, fechaHasta, estadoFiltro);
            movimientos = historial.size();
            cambiosEstado = (int) historial.stream().filter(h -> h.getEstadoNuevo() != null
                    && !h.getEstadoNuevo().isBlank()).count();
            cambiosTecnico = movimientos - cambiosEstado;
        }

        int finalizados = estados.getOrDefault("RESUELTO", 0L).intValue()
                + estados.getOrDefault("CERRADO", 0L).intValue();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("PERIODO", (fechaDesde == null ? "Inicio" : fechaDesde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                + " al " + (fechaHasta == null ? "Actualidad" : fechaHasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        parametros.put("ESTADO_FILTRO", estadoFiltro == null ? "TODOS" : estadoFiltro.replace('_', ' '));
        parametros.put("FECHA_GENERACION", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        parametros.put("TOTAL_TICKETS", tickets.size());
        parametros.put("FINALIZADOS", finalizados);
        parametros.put("PENDIENTES", estados.getOrDefault("PENDIENTE", 0L).intValue());
        parametros.put("EN_PROCESO", estados.getOrDefault("EN_PROCESO", 0L).intValue());
        parametros.put("RESUELTOS", estados.getOrDefault("RESUELTO", 0L).intValue());
        parametros.put("CERRADOS", estados.getOrDefault("CERRADO", 0L).intValue());
        parametros.put("TASA_RESOLUCION", tickets.isEmpty() ? "0%"
                : Math.round(finalizados * 100.0 / tickets.size()) + "%");
        parametros.put("CATEGORIA_PRINCIPAL", principal == null ? "Sin datos" : principal.getKey());
        parametros.put("CATEGORIA_PRINCIPAL_TOTAL", principal == null ? 0 : principal.getValue().intValue());
        parametros.put("MOVIMIENTOS", administrador ? String.valueOf(movimientos) : "N/D");
        parametros.put("CAMBIOS_ESTADO", administrador ? String.valueOf(cambiosEstado) : "N/D");
        parametros.put("CAMBIOS_TECNICO", administrador ? String.valueOf(cambiosTecnico) : "N/D");
        parametros.put("NOTA_HISTORIAL", administrador
                ? "Movimientos por fecha del evento y estado nuevo; tickets por fecha de creación y estado actual."
                : "El historial de movimientos está disponible para administradores.");
        parametros.put("REPORT_LOCALE", Locale.forLanguageTag("es-NI"));

        try (InputStream plantilla = ReporteService.class.getResourceAsStream(
                "/reports/reporte_general.jrxml")) {
            if (plantilla == null) {
                throw new IllegalStateException("No se encontró la plantilla general.");
            }
            JasperReport reporte = JasperCompileManager.compileReport(plantilla);
            return JasperFillManager.fillReport(reporte, parametros, new JREmptyDataSource(1));
        } catch (JRException | IOException e) {
            throw new IllegalStateException("No se pudo preparar el informe general.", e);
        }
    }

    private JasperPrint prepararEstadisticas(
            LocalDate fechaDesde, LocalDate fechaHasta, String estadoFiltro) {
        List<ReporteTicketDTO> tickets = reporteDAO.obtenerTickets(
                fechaDesde, fechaHasta, estadoFiltro, usuarioId);
        Map<String, Long> porEstado = tickets.stream().collect(Collectors.groupingBy(
                ReporteTicketDTO::getEstado, Collectors.counting()));
        Map<String, Long> porCategoria = tickets.stream().collect(Collectors.groupingBy(
                ticket -> ticket.getCategoria() == null || ticket.getCategoria().isBlank()
                        ? "Sin categoría" : ticket.getCategoria(), Collectors.counting()));
        List<Map.Entry<String, Long>> principales = porCategoria.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                        .reversed().thenComparing(Map.Entry::getKey))
                .limit(3).toList();

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("PERIODO",
                (fechaDesde == null ? "Inicio" : fechaDesde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                + " - "
                + (fechaHasta == null ? "Actualidad" : fechaHasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                + " | " + (estadoFiltro == null ? "Todos los estados" : estadoFiltro.replace('_', ' ')));
        parametros.put("FECHA_GENERACION", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        parametros.put("TOTAL_TICKETS", tickets.size());
        parametros.put("PENDIENTES", porEstado.getOrDefault("PENDIENTE", 0L).intValue());
        parametros.put("EN_PROCESO", porEstado.getOrDefault("EN_PROCESO", 0L).intValue());
        parametros.put("RESUELTOS", porEstado.getOrDefault("RESUELTO", 0L).intValue());
        parametros.put("CERRADOS", porEstado.getOrDefault("CERRADO", 0L).intValue());
        for (int i = 0; i < 3; i++) {
            parametros.put("CATEGORIA_" + (i + 1), i < principales.size()
                    ? principales.get(i).getKey() : "Sin datos");
            parametros.put("CATEGORIA_" + (i + 1) + "_TOTAL", i < principales.size()
                    ? principales.get(i).getValue().intValue() : 0);
        }
        parametros.put("OBSERVACION",
                "Indicadores calculados con los filtros seleccionados y los tickets visibles para este usuario.");
        parametros.put("FONDO", FondoEstadisticas.imagen());
        parametros.put("REPORT_LOCALE", Locale.forLanguageTag("es-NI"));

        try (InputStream plantilla = ReporteService.class.getResourceAsStream(
                "/reports/reporte_estadisticas.jrxml")) {
            if (plantilla == null) {
                throw new IllegalStateException("No se encontró la plantilla de estadísticas.");
            }
            JasperReport reporte = JasperCompileManager.compileReport(plantilla);
            return JasperFillManager.fillReport(reporte, parametros, new JREmptyDataSource(1));
        } catch (JRException | IOException e) {
            throw new IllegalStateException("No se pudo preparar el informe de gráficos.", e);
        }
    }

    private boolean esAdministrador() {
        try (Session session = sessionFactory.openSession()) {
            String rol = session.createSelectionQuery("""
                    SELECT r.nombre FROM Usuario u JOIN u.rol r
                    WHERE u.id = :usuario AND u.activo = true
                    """, String.class)
                    .setParameter("usuario", usuarioId)
                    .uniqueResult();
            return "ADMIN".equals(rol);
        }
    }

    public static String crearVistaPrevia(JasperPrint documento) {
        Objects.requireNonNull(documento);
        try {
            StringWriter html = new StringWriter();
            HtmlExporter exportador = new HtmlExporter();
            exportador.setExporterInput(new SimpleExporterInput(documento));
            SimpleHtmlExporterOutput salida = new SimpleHtmlExporterOutput(html);
            Map<String, String> imagenes = new HashMap<>();
            salida.setImageHandler(new HtmlResourceHandler() {
                @Override
                public void handleResource(String id, byte[] datos) {
                    String tipo = datos.length >= 2
                            && (datos[0] & 0xFF) == 0xFF
                            && (datos[1] & 0xFF) == 0xD8
                            ? "image/jpeg" : "image/png";
                    imagenes.put(id, "data:" + tipo + ";base64,"
                            + Base64.getEncoder().encodeToString(datos));
                }

                @Override
                public String getResourcePath(String id) {
                    return imagenes.getOrDefault(id, "");
                }
            });
            exportador.setExporterOutput(salida);
            exportador.exportReport();
            return html.toString();
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo mostrar la vista previa.", e);
        }
    }

    public static Path guardarReporte(JasperPrint documento, Path destino) {
        Objects.requireNonNull(documento);
        if (destino == null) {
            throw new IllegalArgumentException("Debe seleccionar una ubicación para el PDF.");
        }
        try {

            Path archivo = destino.toAbsolutePath().normalize();

            if (!archivo.getFileName().toString()
                    .toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                throw new IllegalArgumentException(
                        "El archivo de destino debe terminar en .pdf."
                );
            }

            Path carpeta = archivo.getParent();

            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }

            // Publicar solamente un PDF completo; conservar el destino si falla.
            Path temporal = Files.createTempFile(carpeta, ".nautaticket-", ".pdf");
            try {
                try (var salida = Files.newOutputStream(temporal)) {
                    JasperExportManager.exportReportToPdfStream(documento, salida);
                }
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporal);
            }

            return archivo;

        } catch (JRException | IOException e) {
            throw new IllegalStateException(
                    "No se pudo generar el reporte PDF.",
                    e
            );
        }
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()
                || estado.equalsIgnoreCase("TODOS")) {
            return null;
        }

        return estado.trim();
    }
}
