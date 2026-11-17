
package ni.edu.uam.nautaticket.service;

import ni.edu.uam.nautaticket.dao.HistorialTicketDAO;
import ni.edu.uam.nautaticket.model.HistorialTicket;
import ni.edu.uam.nautaticket.model.Ticket;
import ni.edu.uam.nautaticket.model.Usuario;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HistorialService {

    private static final Set<String> ESTADOS = Set.of(
            "PENDIENTE",
            "EN_PROCESO",
            "RESUELTO",
            "CERRADO"
    );

    private final HistorialTicketDAO dao;
    private final AutorizacionService autorizacion;

    public HistorialService(SessionFactory sessionFactory) {
        this(sessionFactory, null);
    }

    public HistorialService(SessionFactory sessionFactory, Integer idSolicitante) {
        this.dao = new HistorialTicketDAO(
                Objects.requireNonNull(sessionFactory), null
        );
        this.autorizacion = idSolicitante == null ? null
                : new AutorizacionService(sessionFactory, idSolicitante);
    }

    public void registrarCambioEstado(
            Session session,
            Ticket ticket,
            Usuario actor,
            String anterior,
            String nuevo,
            String comentario) {

        validarEstado(anterior);
        validarEstado(nuevo);

        if (nuevo == null || nuevo.isBlank()) {
            throw new IllegalArgumentException(
                    "El nuevo estado es obligatorio."
            );
        }

        if (Objects.equals(anterior, nuevo)) {
            throw new IllegalArgumentException(
                    "El estado nuevo debe ser diferente del anterior."
            );
        }

        registrar(session, ticket, actor,
                anterior, nuevo, comentario);
    }

    public void registrarCambioTecnico(
            Session session,
            Ticket ticket,
            Usuario actor,
            Integer tecnicoAnteriorId,
            Integer tecnicoNuevoId) {

        if (Objects.equals(tecnicoAnteriorId, tecnicoNuevoId)) {
            throw new IllegalArgumentException(
                    "El técnico nuevo debe ser diferente del anterior."
            );
        }

        String comentario = "Cambio de técnico: "
                + identificador(tecnicoAnteriorId)
                + " -> "
                + identificador(tecnicoNuevoId);

        registrar(session, ticket, actor,
                null, null, comentario);
    }

    private String identificador(Integer id) {
        return id == null ? "sin asignar" : "ID " + id;
    }

    private void registrar(
            Session session,
            Ticket ticket,
            Usuario actor,
            String anterior,
            String nuevo,
            String comentario) {

        Objects.requireNonNull(session, "La sesión es obligatoria");
        Objects.requireNonNull(ticket, "El ticket es obligatorio");
        Objects.requireNonNull(actor, "El usuario es obligatorio");

        if (!session.isOpen() ||
                !session.getTransaction().isActive()) {
            throw new IllegalStateException(
                    "Se necesita una sesión abierta y una transacción activa."
            );
        }

        HistorialTicket movimiento = new HistorialTicket();

        movimiento.setTicket(ticket);
        movimiento.setUsuario(actor);
        movimiento.setEstadoAnterior(anterior);
        movimiento.setEstadoNuevo(nuevo);
        movimiento.setComentario(comentario);
        movimiento.setFechaCambio(LocalDateTime.now());

        dao.guardar(session, movimiento);
    }

    public List<HistorialTicket> historialPorTicket(
            int idTicket,
            LocalDate desde,
            LocalDate hasta,
            String estado) {

        return buscar(idTicket, desde, hasta, estado);
    }

    public List<HistorialTicket> buscar(
            Integer idTicket,
            LocalDate desde,
            LocalDate hasta,
            String estado) {

        verificarLectura();

        if (idTicket != null && idTicket <= 0) {
            throw new IllegalArgumentException(
                    "El ID del ticket debe ser positivo."
            );
        }

        validarFiltros(desde, hasta, estado);

        return dao.buscar(
                idTicket, desde, hasta, estado
        );
    }

    public Map<String, Long> totales(
            LocalDate desde,
            LocalDate hasta,
            String estado) {

        verificarLectura();

        validarFiltros(desde, hasta, estado);

        return dao.contarPorEstado(desde, hasta, estado);
    }

    private void verificarLectura() {
        if (autorizacion == null) {
            throw new SecurityException("Se necesita una identidad para consultar el historial.");
        }
        autorizacion.exigirRol("ADMIN", "TECNICO");
    }

    private void validarFiltros(
            LocalDate desde,
            LocalDate hasta,
            String estado) {

        if (desde != null && hasta != null &&
                desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final."
            );
        }

        validarEstado(estado);
    }

    private void validarEstado(String estado) {
        if (estado != null &&
                !estado.isBlank() &&
                !ESTADOS.contains(estado)) {
            throw new IllegalArgumentException(
                    "Estado no válido: " + estado
            );
        }
    }
}
