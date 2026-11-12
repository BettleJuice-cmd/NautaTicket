package ni.edu.uam.nautaticket.dao;

import ni.edu.uam.nautaticket.model.Categoria;
import ni.edu.uam.nautaticket.model.EstadoTicket;
import ni.edu.uam.nautaticket.model.Ticket;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class TicketDAO {

    private final SessionFactory sessionFactory;
    private final Integer idSolicitante;

    public TicketDAO(SessionFactory sessionFactory) {
        this(sessionFactory, null);
    }

    public TicketDAO(SessionFactory sessionFactory, Integer idSolicitante) {
        this.sessionFactory = sessionFactory;
        this.idSolicitante = idSolicitante;
    }

    public void guardar(Ticket ticket) {
        if (idSolicitante != null && !idSolicitante.equals(ticket.getIdSolicitante())) {
            throw new IllegalArgumentException("El solicitante debe coincidir con el usuario de la sesión.");
        }
        sessionFactory.inTransaction(session -> {
            if (ticket.getEstado() == null) {
                EstadoTicket pendiente = session.createSelectionQuery(
                        "FROM EstadoTicket e WHERE e.nombre = 'PENDIENTE'", EstadoTicket.class
                ).uniqueResultOptional().orElseThrow(() ->
                        new IllegalStateException("Falta el estado PENDIENTE en el catálogo."));
                ticket.setEstado(pendiente);
            }
            session.persist(ticket);
        });
    }

    public List<Ticket> listar() {
        try (Session session = sessionFactory.openSession()) {
            return session.createSelectionQuery("""
                    SELECT t FROM Ticket t JOIN FETCH t.categoria JOIN FETCH t.estado
                    WHERE (:solicitante IS NULL OR t.idSolicitante = :solicitante)
                    ORDER BY t.fechaCreacion ASC, t.idTicket ASC
                    """, Ticket.class)
                    .setParameter("solicitante", idSolicitante, Integer.class)
                    .getResultList();
        }
    }

    public Ticket buscarPorId(Integer id) {
        try (Session session = sessionFactory.openSession()) {
            return session.createSelectionQuery("""
                    SELECT t FROM Ticket t JOIN FETCH t.categoria JOIN FETCH t.estado
                    WHERE t.idTicket = :id
                      AND (:solicitante IS NULL OR t.idSolicitante = :solicitante)
                    """, Ticket.class)
                    .setParameter("id", id)
                    .setParameter("solicitante", idSolicitante, Integer.class)
                    .uniqueResult();
        }
    }

    public List<Ticket> buscarPorCategoria(Integer idCategoria) {
        try (Session session = sessionFactory.openSession()) {
            return session.createSelectionQuery("""
                    SELECT t FROM Ticket t JOIN FETCH t.categoria JOIN FETCH t.estado
                    WHERE t.categoria.idCategoria = :idCategoria
                      AND (:solicitante IS NULL OR t.idSolicitante = :solicitante)
                    ORDER BY t.fechaCreacion ASC, t.idTicket ASC
                    """, Ticket.class)
                    .setParameter("idCategoria", idCategoria)
                    .setParameter("solicitante", idSolicitante, Integer.class)
                    .getResultList();
        }
    }

    public boolean actualizarDatos(Integer id, String titulo,
                                   String descripcion, Integer idCategoria) {
        return sessionFactory.fromTransaction(session -> session.createMutationQuery("""
                UPDATE Ticket t
                SET t.titulo = :titulo, t.descripcion = :descripcion, t.categoria = :categoria
                WHERE t.idTicket = :id
                  AND t.estado.idEstado = (SELECT e.idEstado FROM EstadoTicket e WHERE e.nombre = 'PENDIENTE')
                  AND (:solicitante IS NULL OR t.idSolicitante = :solicitante)
                """)
                .setParameter("titulo", titulo)
                .setParameter("descripcion", descripcion)
                .setParameter("categoria", session.getReference(Categoria.class, idCategoria))
                .setParameter("id", id)
                .setParameter("solicitante", idSolicitante, Integer.class)
                .executeUpdate() == 1);
    }

    public boolean eliminar(Integer id) {
        return sessionFactory.fromTransaction(session -> session.createMutationQuery("""
                DELETE FROM Ticket t
                WHERE t.idTicket = :id
                  AND t.estado.idEstado = (SELECT e.idEstado FROM EstadoTicket e WHERE e.nombre = 'PENDIENTE')
                  AND (:solicitante IS NULL OR t.idSolicitante = :solicitante)
                  AND NOT EXISTS (SELECT 1 FROM HistorialTicket h WHERE h.ticket.idTicket = :id)
                """)
                .setParameter("id", id)
                .setParameter("solicitante", idSolicitante, Integer.class)
                .executeUpdate() == 1);
    }
}
