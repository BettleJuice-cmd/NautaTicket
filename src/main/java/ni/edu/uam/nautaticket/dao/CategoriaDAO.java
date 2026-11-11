package ni.edu.uam.nautaticket.dao;

import ni.edu.uam.nautaticket.model.Categoria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class CategoriaDAO {

    private final SessionFactory sessionFactory;

    public CategoriaDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<Categoria> listarActivas() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "FROM Categoria c WHERE c.activa = true ORDER BY c.nombre",
                    Categoria.class
            ).getResultList();
        }
    }
}
