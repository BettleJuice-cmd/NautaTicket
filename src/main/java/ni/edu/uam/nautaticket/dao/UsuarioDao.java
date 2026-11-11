package ni.edu.uam.nautaticket.dao;

import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class UsuarioDao {

    public void guardar(Usuario usuario) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                session.persist(usuario);
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    public List<Usuario> listar() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT u FROM Usuario u " +
                            "JOIN FETCH u.rol " +
                            "LEFT JOIN FETCH u.departamento " +
                            "ORDER BY u.nombre",
                    Usuario.class
            ).getResultList();
        }
    }

    public Usuario buscarPorId(Integer id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT u FROM Usuario u " +
                                    "JOIN FETCH u.rol " +
                                    "LEFT JOIN FETCH u.departamento " +
                                    "WHERE u.id = :id",
                            Usuario.class
                    ).setParameter("id", id)
                    .uniqueResult();
        }
    }

    public Usuario buscarPorCorreo(String correo) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT u FROM Usuario u " +
                                    "JOIN FETCH u.rol " +
                                    "LEFT JOIN FETCH u.departamento " +
                                    "WHERE u.correo = :correo",
                            Usuario.class
                    ).setParameter("correo", correo)
                    .uniqueResult();
        }
    }

    public void actualizar(Usuario usuario) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                session.merge(usuario);
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    public void desactivar(Integer id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                Usuario usuario = session.find(Usuario.class, id);

                if (usuario != null) {
                    usuario.setActivo(false);
                }

                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }
}
