package ni.edu.uam.nautaticket.dao;

import ni.edu.uam.nautaticket.model.Rol;
import ni.edu.uam.nautaticket.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class RolDao {

    public List<Rol> listar() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Rol ORDER BY nombre",
                    Rol.class
            ).getResultList();
        }
    }

    public Rol buscarPorNombre(String nombre) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Rol WHERE nombre = :nombre",
                            Rol.class
                    ).setParameter("nombre", nombre)
                    .uniqueResult();
        }
    }

    public Rol buscarPorId(Integer id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Rol.class, id);
        }
    }
}
