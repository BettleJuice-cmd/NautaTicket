package ni.edu.uam.nautaticket.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import ni.edu.uam.nautaticket.model.HistorialTicket;
import ni.edu.uam.nautaticket.model.Usuario;

import org.hibernate.SessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ReporteMovimientosService {

    private static final String PLANTILLA =
            "/reports/reporte_movimientos.jrxml";

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm",
                    Locale.forLanguageTag("es-NI")
            );

    private static final DateTimeFormatter FORMATO_DIA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Set<String> ESTADOS = Set.of(
            "PENDIENTE",
            "EN_PROCESO",
            "RESUELTO",
            "CERRADO"
    );

    private final HistorialService historialService;
    private final AutorizacionService autorizacion;

    public ReporteMovimientosService(
            SessionFactory sessionFactory,
            int usuarioId
    ) {

        Objects.requireNonNull(
                sessionFactory,
                "La conexión es obligatoria."
        );

        if (usuarioId <= 0) {
            throw new SecurityException(
                    "Se requiere un usuario autenticado."
            );
        }

        this.historialService = new HistorialService(
                sessionFactory,
                usuarioId
        );

        this.autorizacion = new AutorizacionService(
                sessionFactory,
                usuarioId
        );
    }

    public JasperPrint prepararReporte(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado
    ) {

        // Solo el administrador activo puede generar
        // este tipo de reporte.

        autorizacion.exigirRol("ADMIN");

        if (fechaDesde != null
                && fechaHasta != null
                && fechaDesde.isAfter(fechaHasta)) {

            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final."
            );
        }

        String estadoFiltro = normalizarEstado(estado);

        // Se utiliza el servicio existente.
        // Los resultados incluyen datos de usuarios
        // y tickets mediante JOIN FETCH.

        List<HistorialTicket> movimientos =
                new ArrayList<>(
                        historialService.buscar(
                                null,
                                fechaDesde,
                                fechaHasta,
                                estadoFiltro
                        )
                );

        // El historial original devuelve los más
        // recientes primero. Para la línea de tiempo
        // los presentamos del más antiguo al reciente.

        Collections.reverse(movimientos);

        List<FilaMovimiento> filas = new ArrayList<>();

        Set<Integer> ticketsAfectados = new HashSet<>();

        int cambiosEstado = 0;
        int cambiosTecnico = 0;

        for (HistorialTicket movimiento : movimientos) {

            if (movimiento.getTicket() != null
                    && movimiento.getTicket().getIdTicket() != null) {

                ticketsAfectados.add(
                        movimiento.getTicket().getIdTicket()
                );
            }

            boolean cambioDeEstado =
                    movimiento.getEstadoNuevo() != null
                            && !movimiento.getEstadoNuevo().isBlank();

            if (cambioDeEstado) {
                cambiosEstado++;
            } else {
                cambiosTecnico++;
            }

            filas.add(convertirMovimiento(movimiento));
        }

        Map<String, Object> parametros = new HashMap<>();

        parametros.put(
                "TOTAL_MOVIMIENTOS",
                filas.size()
        );

        parametros.put(
                "TOTAL_TICKETS",
                ticketsAfectados.size()
        );

        parametros.put(
                "CAMBIOS_ESTADO",
                cambiosEstado
        );

        parametros.put(
                "CAMBIOS_TECNICO",
                cambiosTecnico
        );

        parametros.put(
                "FECHA_DESDE",
                fechaDesde == null
                        ? "Sin límite"
                        : fechaDesde.format(FORMATO_DIA)
        );

        parametros.put(
                "FECHA_HASTA",
                fechaHasta == null
                        ? "Sin límite"
                        : fechaHasta.format(FORMATO_DIA)
        );

        parametros.put(
                "ESTADO_FILTRO",
                estadoFiltro == null
                        ? "TODOS"
                        : estadoFiltro.replace("_", " ")
        );

        parametros.put(
                "FECHA_GENERACION",
                LocalDateTime.now().format(FORMATO_FECHA)
        );

        parametros.put(
                "REPORT_LOCALE",
                Locale.forLanguageTag("es-NI")
        );

        try (InputStream plantilla =
                     ReporteMovimientosService.class
                             .getResourceAsStream(PLANTILLA)) {

            if (plantilla == null) {

                throw new IllegalStateException(
                        "No se encontró la plantilla "
                                + PLANTILLA
                );
            }

            JasperReport jasperReport =
                    JasperCompileManager.compileReport(
                            plantilla
                    );

            JRMapCollectionDataSource datos =
                    new JRMapCollectionDataSource(
                            filas.stream().map(ReporteMovimientosService::camposReporte).toList()
                    );

            return JasperFillManager.fillReport(
                    jasperReport,
                    parametros,
                    datos
            );

        } catch (JRException | IOException e) {

            throw new IllegalStateException(
                    "No fue posible generar el reporte "
                            + "de movimientos del historial.",
                    e
            );
        }
    }

    static Map<String, ?> camposReporte(FilaMovimiento fila) {
        Map<String, Object> campos = new HashMap<>();
        campos.put("idHistorial", fila.getIdHistorial());
        campos.put("idTicket", fila.getIdTicket());
        campos.put("tituloTicket", fila.getTituloTicket());
        campos.put("fecha", fila.getFecha());
        campos.put("responsable", fila.getResponsable());
        campos.put("tipo", fila.getTipo());
        campos.put("estadoAnterior", fila.getEstadoAnterior());
        campos.put("estadoNuevo", fila.getEstadoNuevo());
        campos.put("comentario", fila.getComentario());
        return campos;
    }

    private String normalizarEstado(String estado) {

        if (estado == null
                || estado.isBlank()
                || "TODOS".equalsIgnoreCase(estado.trim())) {

            return null;
        }

        String normalizado =
                estado.trim().toUpperCase(Locale.ROOT);

        if (!ESTADOS.contains(normalizado)) {

            throw new IllegalArgumentException(
                    "El estado seleccionado no es válido."
            );
        }

        return normalizado;
    }

    private FilaMovimiento convertirMovimiento(
            HistorialTicket movimiento
    ) {

        Usuario responsable = movimiento.getUsuario();

        String nombreResponsable = "Usuario desconocido";

        if (responsable != null) {

            String nombre = texto(responsable.getNombre());
            String apellido = texto(responsable.getApellido());

            nombreResponsable =
                    (nombre + " " + apellido).trim();

            if (nombreResponsable.isBlank()) {

                nombreResponsable =
                        texto(responsable.getCorreo());
            }
        }

        String estadoAnterior =
                movimiento.getEstadoAnterior();

        String estadoNuevo =
                movimiento.getEstadoNuevo();

        boolean esCambioEstado =
                estadoNuevo != null
                        && !estadoNuevo.isBlank();

        String tipo = esCambioEstado
                ? "CAMBIO DE ESTADO"
                : "CAMBIO DE TECNICO";

        String anterior = esCambioEstado
                ? mostrarEstado(estadoAnterior)
                : "SIN CAMBIO";

        String nuevo = esCambioEstado
                ? mostrarEstado(estadoNuevo)
                : "SIN CAMBIO";

        String comentario = texto(
                movimiento.getComentario()
        );

        if (comentario.isBlank()) {
            comentario = "Sin observaciones registradas.";
        }

        String titulo = "Ticket sin título";
        Integer idTicket = null;

        if (movimiento.getTicket() != null) {

            idTicket =
                    movimiento.getTicket().getIdTicket();

            String tituloReal =
                    texto(movimiento.getTicket().getTitulo());

            if (!tituloReal.isBlank()) {
                titulo = tituloReal;
            }
        }

        String fecha = movimiento.getFechaCambio() == null
                ? "Fecha no disponible"
                : movimiento.getFechaCambio().format(
                FORMATO_FECHA
        );

        return new FilaMovimiento(
                movimiento.getIdHistorial(),
                idTicket,
                titulo,
                fecha,
                nombreResponsable,
                tipo,
                anterior,
                nuevo,
                comentario
        );
    }

    private String mostrarEstado(String estado) {

        if (estado == null || estado.isBlank()) {
            return "SIN ESTADO";
        }

        return estado.replace("_", " ");
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    public static class FilaMovimiento {

        private final Integer idHistorial;
        private final Integer idTicket;
        private final String tituloTicket;
        private final String fecha;
        private final String responsable;
        private final String tipo;
        private final String estadoAnterior;
        private final String estadoNuevo;
        private final String comentario;

        public FilaMovimiento(
                Integer idHistorial,
                Integer idTicket,
                String tituloTicket,
                String fecha,
                String responsable,
                String tipo,
                String estadoAnterior,
                String estadoNuevo,
                String comentario
        ) {

            this.idHistorial = idHistorial;
            this.idTicket = idTicket;
            this.tituloTicket = tituloTicket;
            this.fecha = fecha;
            this.responsable = responsable;
            this.tipo = tipo;
            this.estadoAnterior = estadoAnterior;
            this.estadoNuevo = estadoNuevo;
            this.comentario = comentario;
        }

        public Integer getIdHistorial() {
            return idHistorial;
        }

        public Integer getIdTicket() {
            return idTicket;
        }

        public String getTituloTicket() {
            return tituloTicket;
        }

        public String getFecha() {
            return fecha;
        }

        public String getResponsable() {
            return responsable;
        }

        public String getTipo() {
            return tipo;
        }

        public String getEstadoAnterior() {
            return estadoAnterior;
        }

        public String getEstadoNuevo() {
            return estadoNuevo;
        }

        public String getComentario() {
            return comentario;
        }
    }
}
