
package ni.edu.uam.nautaticket.dao;

import ni.edu.uam.nautaticket.dto.ReporteTicketDTO;
import ni.edu.uam.nautaticket.model.Ticket;
import ni.edu.uam.nautaticket.model.Usuario;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.LocalDate;
import java.util.List;

public class ReporteDAO {

    private final SessionFactory sessionFactory;

    public ReporteDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<ReporteTicketDTO> obtenerTickets(
            LocalDate desde,
            LocalDate hasta,
            String estado) {

        if (desde != null && hasta != null
                && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final."
            );
        }

        StringBuilder hql = new StringBuilder("""
                SELECT t FROM Ticket t
                JOIN FETCH t.solicitante s
                LEFT JOIN FETCH s.departamento
                JOIN FETCH t.categoria
                LEFT JOIN FETCH t.tecnico
                JOIN FETCH t.estado
                WHERE 1 = 1
                """);

        if (desde != null) {
            hql.append(" AND t.fechaCreacion >= :desde");
        }

        if (hasta != null) {
            hql.append(" AND t.fechaCreacion < :hasta");
        }

        if (estado != null && !estado.isBlank()) {
            hql.append(" AND t.estado.nombre = :estado");
        }

        hql.append(
                " ORDER BY t.fechaCreacion ASC, t.idTicket ASC"
        );

        try (Session session = sessionFactory.openSession()) {

            var query = session.createQuery(
                    hql.toString(),
                    Ticket.class
            );

            if (desde != null) {
                query.setParameter(
                        "desde",
                        desde.atStartOfDay()
                );
            }

            if (hasta != null) {
                query.setParameter(
                        "hasta",
                        hasta.plusDays(1).atStartOfDay()
                );
            }

            if (estado != null && !estado.isBlank()) {
                query.setParameter("estado", estado.trim());
            }

            return query.getResultList()
                    .stream()
                    .map(this::convertirDTO)
                    .toList();
        }
    }

    private ReporteTicketDTO convertirDTO(Ticket ticket) {

        Usuario solicitante = ticket.getSolicitante();
        Usuario tecnico = ticket.getTecnico();

        String departamento = "Sin departamento";

        if (solicitante.getDepartamento() != null) {
            departamento =
                    solicitante.getDepartamento().getNombre();
        }

        String nombreTecnico = "Sin asignar";

        if (tecnico != null) {
            nombreTecnico = tecnico.getNombre()
                    + " " + tecnico.getApellido();
        }

        return new ReporteTicketDTO(
                ticket.getIdTicket(),
                departamento,
                ticket.getTitulo(),
                ticket.getCategoria().getNombre(),
                nombreTecnico,
                ticket.getEstado().getNombre(),
                ticket.getFechaCreacion(),
                ticket.getFechaResolucion()
        );
    }
}
