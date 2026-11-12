
package ni.edu.uam.nautaticket.util;

import ni.edu.uam.nautaticket.model.Departamento;
import ni.edu.uam.nautaticket.model.Rol;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.model.EstadoTicket;
import ni.edu.uam.nautaticket.model.AsignacionTicket;
import ni.edu.uam.nautaticket.model.Categoria;
import ni.edu.uam.nautaticket.model.HistorialTicket;
import ni.edu.uam.nautaticket.model.Ticket;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static SessionFactory sessionFactory;

    private static SessionFactory crearSessionFactory() {

        String url = System.getenv().getOrDefault(
                "DB_URL",
                "jdbc:postgresql://localhost:5432/nautaticket_db"
        );

        String usuario = System.getenv().getOrDefault(
                "DB_USER", "postgres"
        );

        String password = System.getenv("DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Configura la variable DB_PASSWORD"
            );
        }

        return configuracionBase()
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", usuario)
                .setProperty("hibernate.connection.password", password)
                .buildSessionFactory();
    }

    static Configuration configuracionBase() {
        return new Configuration()
                .configure("hibernate.cfg.xml")
                .addAnnotatedClass(Departamento.class)
                .addAnnotatedClass(Rol.class)
                .addAnnotatedClass(Usuario.class)
                .addAnnotatedClass(EstadoTicket.class)
                .addAnnotatedClass(Categoria.class)
                .addAnnotatedClass(Ticket.class)
                .addAnnotatedClass(AsignacionTicket.class)
                .addAnnotatedClass(HistorialTicket.class);
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            sessionFactory = crearSessionFactory();
        }
        return sessionFactory;
    }

    public static synchronized void cerrar() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
