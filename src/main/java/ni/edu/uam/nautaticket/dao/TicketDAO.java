
package ni.edu.uam.nautaticket.dao;

import ni.edu.uam.nautaticket.model.Ticket;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class TicketDAO {

    private final SessionFactory sessionFactory;

    public TicketDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void guardar(Ticket ticket) {
        Transaction tx = null;

        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.persist(ticket);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public List<Ticket> listar() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "SELECT t FROM Ticket t JOIN FETCH t.categoria ORDER BY t.fechaCreacion ASC, t.idTicket ASC",
                    Ticket.class
            ).getResultList();
        }
    }

    public Ticket buscarPorId(Integer id) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "SELECT t FROM Ticket t JOIN FETCH t.categoria WHERE t.idTicket = :id",
                            Ticket.class
                    )
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }

    public List<Ticket> buscarPorCategoria(Integer idCategoria) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "SELECT t FROM Ticket t JOIN FETCH t.categoria " +
                                    "WHERE t.categoria.idCategoria = :idCategoria " +
                                    "ORDER BY t.fechaCreacion ASC, t.idTicket ASC",
                            Ticket.class
                    )
                    .setParameter("idCategoria", idCategoria)
                    .getResultList();
        }
    }

    public boolean actualizarDatos(Integer id, String titulo,
                                   String descripcion, Integer idCategoria) {
        Transaction tx = null;

        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();

            int filas = session.createMutationQuery(
                            "UPDATE Ticket t SET " +
                                    "t.titulo = :titulo, " +
                                    "t.descripcion = :descripcion, " +
                                    "t.categoria = (SELECT c FROM Categoria c WHERE c.idCategoria = :idCategoria) " +
                                    "WHERE t.idTicket = :id AND t.estado = 'PENDIENTE'"
                    )
                    .setParameter("titulo", titulo)
                    .setParameter("descripcion", descripcion)
                    .setParameter("idCategoria", idCategoria)
                    .setParameter("id", id)
                    .executeUpdate();

            tx.commit();
            return filas > 0;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public boolean eliminar(Integer id) {
        Transaction tx = null;

        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();

            int filas = session.createMutationQuery(
                            "DELETE FROM Ticket t " +
                                    "WHERE t.idTicket = :id " +
                                    "AND t.estado = 'PENDIENTE' " +
                                    "AND NOT EXISTS (" +
                                    "SELECT h.idHistorial FROM HistorialTicket h " +
                                    "WHERE h.ticket.idTicket = :id)"
                    )
                    .setParameter("id", id)
                    .executeUpdate();

            tx.commit();
            return filas > 0;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
