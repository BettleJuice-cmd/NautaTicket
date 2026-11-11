
package ni.edu.uam.nautaticket.dao;

import ni.edu.uam.nautaticket.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class AsignacionTicketDao {

    private final SessionFactory sessionFactory;

    public AsignacionTicketDao() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    public record TicketFila(
            int idTicket,
            String titulo,
            LocalDateTime fechaCreacion,
            String estado
    ) {}

    public List<TicketFila> listarPendientes() {
        String sql = """
                SELECT t.id_ticket, t.titulo,
                       t.fecha_creacion, e.nombre
                FROM tickets t
                JOIN estados_ticket e
                    ON t.id_estado = e.id_estado
                WHERE e.nombre = 'PENDIENTE'
                  AND t.id_tecnico IS NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM asignaciones_ticket a
                      WHERE a.id_ticket = t.id_ticket
                        AND a.fecha_fin IS NULL
                  )
                ORDER BY t.fecha_creacion ASC,
                         t.id_ticket ASC
                """;

        try (Session session = sessionFactory.openSession()) {
            List<Object[]> resultados =
                    session.createNativeQuery(sql, Object[].class)
                            .getResultList();

            return resultados.stream()
                    .map(this::convertirFila)
                    .toList();
        }
    }

    public List<TicketFila> listarMisTickets(int idTecnico) {
        String sql = """
                SELECT t.id_ticket, t.titulo,
                       t.fecha_creacion, e.nombre
                FROM tickets t
                JOIN estados_ticket e
                    ON t.id_estado = e.id_estado
                WHERE t.id_tecnico = :tecnico
                  AND e.nombre IN ('PENDIENTE', 'EN_PROCESO')
                ORDER BY t.fecha_creacion ASC,
                         t.id_ticket ASC
                """;

        try (Session session = sessionFactory.openSession()) {
            List<Object[]> resultados =
                    session.createNativeQuery(sql, Object[].class)
                            .setParameter("tecnico", idTecnico)
                            .getResultList();

            return resultados.stream()
                    .map(this::convertirFila)
                    .toList();
        }
    }

    public Integer buscarSiguienteFIFO(Session session) {
        String sql = """
                SELECT t.id_ticket
                FROM tickets t
                JOIN estados_ticket e
                    ON t.id_estado = e.id_estado
                WHERE e.nombre = 'PENDIENTE'
                  AND t.id_tecnico IS NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM asignaciones_ticket a
                      WHERE a.id_ticket = t.id_ticket
                        AND a.fecha_fin IS NULL
                  )
                ORDER BY t.fecha_creacion ASC,
                         t.id_ticket ASC
                LIMIT 1
                FOR UPDATE OF t SKIP LOCKED
                """;

        Object resultado = session.createNativeQuery(sql)
                .uniqueResult();

        return resultado == null
                ? null
                : ((Number) resultado).intValue();
    }

    private TicketFila convertirFila(Object[] fila) {
        Object fecha = fila[2];

        LocalDateTime fechaCreacion =
                fecha instanceof LocalDateTime local
                        ? local
                        : ((Timestamp) fecha).toLocalDateTime();

        return new TicketFila(
                ((Number) fila[0]).intValue(),
                (String) fila[1],
                fechaCreacion,
                (String) fila[3]
        );
    }
}
