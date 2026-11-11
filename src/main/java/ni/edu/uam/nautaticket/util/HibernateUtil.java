package ni.edu.uam.nautaticket.util;

import ni.edu.uam.nautaticket.model.Departamento;
import ni.edu.uam.nautaticket.model.Rol;
import ni.edu.uam.nautaticket.model.Usuario;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final SessionFactory sessionFactory =
            crearSessionFactory();

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


        return new Configuration()
                .configure("hibernate.cfg.xml")
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", usuario)
                .setProperty("hibernate.connection.password", password)
                .addAnnotatedClass(Departamento.class)
                .addAnnotatedClass(Rol.class)
                .addAnnotatedClass(Usuario.class)
                .buildSessionFactory();

    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void cerrar() {
        if (!sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
