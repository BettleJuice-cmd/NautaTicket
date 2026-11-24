package ni.edu.uam.nautaticket.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;

import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.ReporteService;
import ni.edu.uam.nautaticket.service.ReporteMovimientosService;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import org.hibernate.SessionFactory;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebReporteBridge {

    private static final Logger LOG =
            Logger.getLogger(WebReporteBridge.class.getName());

    private static final Set<String> ESTADOS =
            Set.of(
                    "PENDIENTE",
                    "EN_PROCESO",
                    "RESUELTO",
                    "CERRADO"
            );

    private final WebEngine engine;
    private final Stage ventanaPrincipal;
    private final String paginaPermitida;
    private final int usuarioId;
    private final SessionFactory sessionFactory;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicBoolean activo =
            new AtomicBoolean(true);

    private boolean procesando;
    private JasperPrint reporteEnVista;
    private String tipoEnVista;

    public WebReporteBridge(
            WebEngine engine,
            Stage ventanaPrincipal,
            String paginaPermitida,
            Usuario usuario
    ) {
        if (usuario == null || usuario.getId() == null) {
            throw new SecurityException(
                    "Se requiere una sesión autenticada."
            );
        }

        this.engine = engine;
        this.ventanaPrincipal = ventanaPrincipal;
        this.paginaPermitida = paginaPermitida;
        this.usuarioId = usuario.getId();

        this.sessionFactory =
                HibernateUtil.getSessionFactory();
    }

    public void desactivar() {
        activo.set(false);
        reporteEnVista = null;
        tipoEnVista = null;
    }

    public void generarReporte(
            String desde,
            String hasta,
            String estado,
            String tipo
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        if (procesando) {
            enviarResultado(
                    false,
                    "Hay un reporte en proceso.",
                    "",
                    ""
            );
            return;
        }

        final LocalDate fechaDesde;
        final LocalDate fechaHasta;
        final String estadoFiltro;
        final String tipoReporte;

        try {
            fechaDesde = convertirFecha(desde);
            fechaHasta = convertirFecha(hasta);
            estadoFiltro = convertirEstado(estado);
            tipoReporte = tipo == null ? "" : tipo.trim().toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("GENERAL", "TICKETS", "HISTORIAL", "GRAFICOS").contains(tipoReporte)) {
                throw new IllegalArgumentException("El tipo de informe no es válido.");
            }

            if (fechaDesde != null &&
                    fechaHasta != null &&
                    fechaDesde.isAfter(fechaHasta)) {

                throw new IllegalArgumentException(
                        "La fecha inicial no puede ser posterior a la final."
                );
            }

        } catch (IllegalArgumentException e) {
            enviarResultado(false, e.getMessage(), "", "");
            return;
        }

        procesando = true;
        reporteEnVista = null;
        tipoEnVista = null;
        Task<JasperPrint> tarea = new Task<>() {

            @Override
            protected JasperPrint call() {

                if (!activo.get()) {
                    throw new SecurityException(
                            "La sesión ya no está disponible."
                    );
                }

                if ("HISTORIAL".equals(tipoReporte)) {
                    return new ReporteMovimientosService(sessionFactory, usuarioId)
                            .prepararReporte(fechaDesde, fechaHasta, estadoFiltro);
                }
                return new ReporteService(sessionFactory, usuarioId)
                        .prepararReporte(fechaDesde, fechaHasta, estadoFiltro, tipoReporte);
            }
        };

        tarea.setOnSucceeded(evento -> {

            if (!paginaAutorizada()) {
                procesando = false;
                return;
            }
            try {
                JasperPrint documento = tarea.getValue();
                String vistaPrevia = ReporteService.crearVistaPrevia(documento);

                reporteEnVista = documento;
                tipoEnVista = tipoReporte;
                procesando = false;
                enviarResultado(
                        true,
                        "Vista previa lista. Revisa el contenido y guarda el PDF cuando estés conforme.",
                        "",
                        vistaPrevia
                );
            } catch (RuntimeException error) {
                procesando = false;
                LOG.log(Level.SEVERE, "Error preparando vista previa", error);
                enviarResultado(false, "No se pudo preparar la vista previa.", "", "");
            }
        });

        tarea.setOnFailed(evento -> {

            procesando = false;

            Throwable error = tarea.getException();

            LOG.log(
                    Level.SEVERE,
                    "Error generando PDF",
                    error
            );

            String mensaje =
                    error instanceof IllegalArgumentException
                            || error instanceof SecurityException
                            ? error.getMessage()
                            : "No se pudo generar el reporte PDF.";

            enviarResultado(false, mensaje, "", "");
        });

        Thread hilo = new Thread(
                tarea,
                "nautaticket-generador-pdf"
        );

        hilo.setDaemon(true);
        hilo.start();
    }

    private LocalDate convertirFecha(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(texto.trim());

        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "La fecha ingresada no es válida."
            );
        }
    }

    private String convertirEstado(String estado) {

        if (estado == null ||
                estado.isBlank() ||
                "TODOS".equalsIgnoreCase(estado)) {

            return null;
        }

        String normalizado =
                estado.trim().toUpperCase();

        if (!ESTADOS.contains(normalizado)) {
            throw new IllegalArgumentException(
                    "El estado seleccionado no es válido."
            );
        }

        return normalizado;
    }

    private boolean paginaAutorizada() {

        return activo.get() &&
                paginaPermitida.equals(
                        engine.getLocation()
                );
    }

    public void guardarReporte() {
        if (!paginaAutorizada() || procesando) {
            return;
        }

        if (reporteEnVista == null) {
            enviarResultado(
                    false,
                    "Primero genera una vista previa del reporte.",
                    "",
                    ""
            );
            return;
        }

        javafx.stage.FileChooser selector = new javafx.stage.FileChooser();
        String nombre = switch (tipoEnVista) {
            case "TICKETS" -> "Tickets";
            case "HISTORIAL" -> "Historial";
            case "GRAFICOS" -> "Estadisticas";
            default -> "General";
        };
        selector.setTitle("Guardar informe de " + nombre.toLowerCase(java.util.Locale.ROOT));
        selector.setInitialFileName("NautaTicket_Reporte_" + nombre + ".pdf");
        selector.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter(
                        "Documento PDF (*.pdf)", "*.pdf"
                )
        );

        java.io.File destino = selector.showSaveDialog(ventanaPrincipal);
        if (destino == null) {
            enviarResultado(false, "No se seleccionó una ubicación para el PDF.", "", "");
            return;
        }

        JasperPrint documento = reporteEnVista;
        procesando = true;
        Task<java.nio.file.Path> tarea = new Task<>() {
            @Override
            protected java.nio.file.Path call() {
                if (!activo.get()) {
                    throw new SecurityException("La sesión ya no está disponible.");
                }
                return ReporteService.guardarReporte(documento, destino.toPath());
            }
        };

        tarea.setOnSucceeded(evento -> {
            procesando = false;
            enviarResultado(
                    true,
                    "Reporte guardado correctamente.",
                    tarea.getValue().toString(),
                    ""
            );
        });
        tarea.setOnFailed(evento -> {
            procesando = false;
            Throwable error = tarea.getException();
            LOG.log(Level.SEVERE, "Error guardando PDF", error);
            enviarResultado(
                    false,
                    error == null || error.getMessage() == null
                            ? "No se pudo guardar el reporte PDF."
                            : error.getMessage(),
                    "",
                    ""
            );
        });

        Thread hilo = new Thread(tarea, "nautaticket-guardar-pdf");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void enviarResultado(
            boolean correcto,
            String mensaje,
            String archivo,
            String vistaPrevia
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        Map<String, Object> respuesta =
                new LinkedHashMap<>();

        respuesta.put("correcto", correcto);
        respuesta.put("mensaje", mensaje);
        respuesta.put("archivo", archivo);
        respuesta.put("vistaPrevia", vistaPrevia);

        try {

            String json =
                    mapper.writeValueAsString(respuesta);

            String argumento =
                    mapper.writeValueAsString(json);

            engine.executeScript(
                    "if (typeof window.nautaReporteResultado " +
                            "=== 'function') {" +
                            "window.nautaReporteResultado(" +
                            "JSON.parse(" + argumento + "));" +
                            "}"
            );

        } catch (Exception e) {

            LOG.log(
                    Level.SEVERE,
                    "No se pudo comunicar el resultado del PDF",
                    e
            );
        }
    }
}
