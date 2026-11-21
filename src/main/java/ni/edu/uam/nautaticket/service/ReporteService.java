
package ni.edu.uam.nautaticket.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;

import ni.edu.uam.nautaticket.dao.ReporteDAO;
import ni.edu.uam.nautaticket.dto.ReporteTicketDTO;

import org.hibernate.SessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final int usuarioId;

    public ReporteService(SessionFactory sessionFactory, int usuarioId) {
        if (usuarioId <= 0) {
            throw new IllegalArgumentException("El usuario del reporte no es válido.");
        }
        this.reporteDAO = new ReporteDAO(
                Objects.requireNonNull(sessionFactory)
        );
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

        if (fechaDesde != null && fechaHasta != null
                && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final."
            );
        }

        String estadoFiltro = normalizarEstado(estado);

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

    public static String crearVistaPrevia(JasperPrint documento) {
        Objects.requireNonNull(documento);
        try {
            StringWriter html = new StringWriter();
            HtmlExporter exportador = new HtmlExporter();
            exportador.setExporterInput(new SimpleExporterInput(documento));
            exportador.setExporterOutput(new SimpleHtmlExporterOutput(html));
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
