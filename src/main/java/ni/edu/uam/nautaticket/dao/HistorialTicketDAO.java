
package ni.edu.uam.nautaticket.dao;

import ni.edu.uam.nautaticket.model.HistorialTicket;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistorialTicketDAO {

    private final SessionFactory sessionFactory;

    public HistorialTicketDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void guardar(Session session, HistorialTicket movimiento) {
        session.persist(movimiento);
    }

    public List<HistorialTicket> buscarPorTicket(
            int idTicket,
            LocalDate desde,
            LocalDate hasta,
            String estado) {

        StringBuilder hql = new StringBuilder("""
                SELECT h FROM HistorialTicket h
                JOIN FETCH h.ticket
                JOIN FETCH h.usuario
                WHERE h.ticket = :ticket
                """);

        if (desde != null) {
            hql.append(" AND h.fechaCambio >= :desde");
        }

        if (hasta != null) {
            hql.append(" AND h.fechaCambio < :hasta");
        }

        if (estado != null && !estado.isBlank()) {
            hql.append(" AND h.estadoNuevo = :estado");
        }

        hql.append(" ORDER BY h.fechaCambio DESC, h.idHistorial DESC");

        try (Session session = sessionFactory.openSession()) {

            var ticket = session.getReference(
                    ni.edu.uam.nautaticket.model.Ticket.class,
                    idTicket
            );

            var query = session.createQuery(
                    hql.toString(),
                    HistorialTicket.class
            );

            query.setParameter("ticket", ticket);

            if (desde != null) {
                query.setParameter("desde", desde.atStartOfDay());
            }

            if (hasta != null) {
                query.setParameter(
                        "hasta",
                        hasta.plusDays(1).atStartOfDay()
                );
            }

            if (estado != null && !estado.isBlank()) {
                query.setParameter("estado", estado);
            }

            return query.getResultList();
        }
    }

    public Map<String, Long> contarPorEstado(
            LocalDate desde,
            LocalDate hasta,
            String estado) {

        StringBuilder sql = new StringBuilder("""
                SELECT e.nombre, COUNT(*)
                FROM tickets t
                JOIN estados_ticket e
                    ON e.id_estado = t.id_estado
                WHERE 1 = 1
                """);

        if (desde != null) {
            sql.append(" AND t.fecha_creacion >= :desde");
        }

        if (hasta != null) {
            sql.append(" AND t.fecha_creacion < :hasta");
        }

        if (estado != null && !estado.isBlank()) {
            sql.append(" AND e.nombre = :estado");
        }

        sql.append(" GROUP BY e.nombre");

        try (Session session = sessionFactory.openSession()) {

            var query = session.createNativeQuery(
                    sql.toString(),
                    Object[].class
            );

            if (desde != null) {
                query.setParameter("desde", desde.atStartOfDay());
            }

            if (hasta != null) {
                query.setParameter(
                        "hasta",
                        hasta.plusDays(1).atStartOfDay()
                );
            }

            if (estado != null && !estado.isBlank()) {
                query.setParameter("estado", estado);
            }

            Map<String, Long> totales = new HashMap<>();

            for (Object[] fila : query.getResultList()) {
                totales.put(
                        (String) fila[0],
                        ((Number) fila[1]).longValue()
                );
            }

            return totales;
        }
    }
}
