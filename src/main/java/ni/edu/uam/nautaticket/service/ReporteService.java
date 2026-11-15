
package ni.edu.uam.nautaticket.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import ni.edu.uam.nautaticket.dao.ReporteDAO;
import ni.edu.uam.nautaticket.dto.ReporteTicketDTO;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import org.hibernate.SessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReporteService {

    private static final String PLANTILLA =
            "/reports/reporte_tickets.jrxml";

    private final ReporteDAO reporteDAO;

    public ReporteService() {
        this(HibernateUtil.getSessionFactory());
    }

    public ReporteService(SessionFactory sessionFactory) {
        this.reporteDAO = new ReporteDAO(
                Objects.requireNonNull(sessionFactory)
        );
    }

    public Path generarReporte(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            Path destino) {

        if (destino == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una ubicación para el PDF."
            );
        }

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
                        estadoFiltro
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

            JasperExportManager.exportReportToPdfFile(
                    documento,
                    archivo.toString()
            );

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
