package ni.edu.uam.nautaticket.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.scene.web.WebEngine;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.ReporteService;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import org.hibernate.SessionFactory;

import java.io.File;
import java.nio.file.Path;
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
    }

    public void generarReporte(
            String desde,
            String hasta,
            String estado
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        if (procesando) {
            enviarResultado(
                    false,
                    "Hay un reporte en proceso.",
                    ""
            );
            return;
        }

        final LocalDate fechaDesde;
        final LocalDate fechaHasta;
        final String estadoFiltro;

        try {
            fechaDesde = convertirFecha(desde);
            fechaHasta = convertirFecha(hasta);
            estadoFiltro = convertirEstado(estado);

            if (fechaDesde != null &&
                    fechaHasta != null &&
                    fechaDesde.isAfter(fechaHasta)) {

                throw new IllegalArgumentException(
                        "La fecha inicial no puede ser posterior a la final."
                );
            }

        } catch (IllegalArgumentException e) {
            enviarResultado(false, e.getMessage(), "");
            return;
        }

        // La ubicación se selecciona desde Windows,
        // no se recibe una ruta arbitraria de JavaScript.
        FileChooser selector = new FileChooser();

        selector.setTitle("Guardar reporte de NautaTicket");

        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Documento PDF (*.pdf)",
                        "*.pdf"
                )
        );

        selector.setInitialFileName(
                "NautaTicket_Reporte_Tickets.pdf"
        );

        File destino = selector.showSaveDialog(
                ventanaPrincipal
        );

        if (destino == null) {
            enviarResultado(
                    false,
                    "Se canceló la generación del reporte.",
                    ""
            );
            return;
        }

        procesando = true;

        final Path rutaDestino = destino.toPath();

        Task<Path> tarea = new Task<>() {

            @Override
            protected Path call() {

                if (!activo.get()) {
                    throw new SecurityException(
                            "La sesión ya no está disponible."
                    );
                }

                ReporteService servicio =
                        new ReporteService(
                                sessionFactory,
                                usuarioId
                        );

                return servicio.generarReporte(
                        fechaDesde,
                        fechaHasta,
                        estadoFiltro,
                        rutaDestino
                );
            }
        };

        tarea.setOnSucceeded(evento -> {

            procesando = false;

            Path archivo = tarea.getValue();

            enviarResultado(
                    true,
                    "Reporte generado correctamente.",
                    archivo.toString()
            );
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

            enviarResultado(false, mensaje, "");
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

    private void enviarResultado(
            boolean correcto,
            String mensaje,
            String archivo
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        Map<String, Object> respuesta =
                new LinkedHashMap<>();

        respuesta.put("correcto", correcto);
        respuesta.put("mensaje", mensaje);
        respuesta.put("archivo", archivo);

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
