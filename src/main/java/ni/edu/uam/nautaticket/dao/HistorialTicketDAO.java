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

    // La sesión y la transacción pertenecen a la operación que modifica el ticket.
    public void guardar(Session session, HistorialTicket movimiento) {
        session.persist(movimiento);
    }

    public List<HistorialTicket> buscarPorTicket(int idTicket, LocalDate desde,
                                                LocalDate hasta, String estado) {
        StringBuilder hql = new StringBuilder("""
                select h from HistorialTicket h
                join fetch h.ticket
                join fetch h.usuario
                where id(h.ticket) = :idTicket
                """);
        if (desde != null) hql.append(" and h.fechaCambio >= :desde");
        if (hasta != null) hql.append(" and h.fechaCambio < :hasta");
        if (estado != null && !estado.isBlank()) hql.append(" and h.estadoNuevo = :estado");
        hql.append(" order by h.fechaCambio desc, h.idHistorial desc");

        try (Session session = sessionFactory.openSession()) {
            var query = session.createQuery(hql.toString(), HistorialTicket.class);
            query.setParameter("idTicket", idTicket);
            if (desde != null) query.setParameter("desde", desde.atStartOfDay());
            if (hasta != null) query.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
            if (estado != null && !estado.isBlank()) query.setParameter("estado", estado);
            return query.getResultList();
        }
    }

    // La fecha del filtro es la fecha de creación del ticket; se cuenta su estado actual.
    public Map<String, Long> contarPorEstado(LocalDate desde, LocalDate hasta, String estado) {
        StringBuilder sql = new StringBuilder("select e.nombre, count(*) from tickets t join estados_ticket e on e.id_estado = t.id_estado where 1=1");
        if (desde != null) sql.append(" and t.fecha_creacion >= :desde");
        if (hasta != null) sql.append(" and t.fecha_creacion < :hasta");
        if (estado != null && !estado.isBlank()) sql.append(" and e.nombre = :estado");
        sql.append(" group by e.nombre");
        try (Session session = sessionFactory.openSession()) {
            var query = session.createNativeQuery(sql.toString(), Object[].class);
            if (desde != null) query.setParameter("desde", desde.atStartOfDay());
            if (hasta != null) query.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
            if (estado != null && !estado.isBlank()) query.setParameter("estado", estado);
            Map<String, Long> totales = new HashMap<>();
            for (Object[] fila : query.getResultList()) {
                totales.put((String) fila[0], ((Number) fila[1]).longValue());
            }
            return totales;
        }
    }
}
