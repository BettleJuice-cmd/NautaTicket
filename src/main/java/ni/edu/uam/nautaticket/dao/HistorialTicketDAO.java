
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
    private final Integer idSolicitante;

    public HistorialTicketDAO(SessionFactory sessionFactory) {
        this(sessionFactory, null);
    }

    public HistorialTicketDAO(SessionFactory sessionFactory, Integer idSolicitante) {
        this.sessionFactory = sessionFactory;
        this.idSolicitante = idSolicitante;
    }

    public void guardar(Session session, HistorialTicket movimiento) {
        session.persist(movimiento);
    }

    public List<HistorialTicket> buscarPorTicket(
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

        StringBuilder hql = new StringBuilder("""
                SELECT h FROM HistorialTicket h
                JOIN FETCH h.ticket t
                JOIN FETCH h.usuario
                WHERE (:solicitante IS NULL OR t.solicitante.id = :solicitante)
                """);

        if (idTicket != null) {
            hql.append(" AND t.idTicket = :ticket");
        }

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

            var query = session.createQuery(
                    hql.toString(),
                    HistorialTicket.class
            );

            if (idTicket != null) {
                query.setParameter("ticket", idTicket);
            }
            query.setParameter("solicitante", idSolicitante, Integer.class);

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

        StringBuilder hql = new StringBuilder("""
                SELECT e.nombre, COUNT(t.idTicket)
                FROM Ticket t
                JOIN t.estado e
                WHERE 1 = 1
                """);

        if (desde != null) {
            hql.append(" AND t.fechaCreacion >= :desde");
        }

        if (hasta != null) {
            hql.append(" AND t.fechaCreacion < :hasta");
        }

        if (estado != null && !estado.isBlank()) {
            hql.append(" AND e.nombre = :estado");
        }

        if (idSolicitante != null) {
            hql.append(" AND t.solicitante.id = :solicitante");
        }

        hql.append(" GROUP BY e.nombre");

        try (Session session = sessionFactory.openSession()) {

            var query = session.createQuery(
                    hql.toString(),
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

            if (idSolicitante != null) {
                query.setParameter("solicitante", idSolicitante);
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
