
package ni.edu.uam.nautaticket.util;

import org.hibernate.Session;

public class TestHibernateConexion {

    public static void main(String[] args) {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            String baseDatos = session.doReturningWork(connection -> {
                try (var statement = connection.prepareStatement(
                        "SELECT current_database()");
                     var resultado = statement.executeQuery()) {

                    resultado.next();
                    return resultado.getString(1);
                }
            });

            System.out.println("HIBERNATE CONECTADO CORRECTAMENTE");
            System.out.println("Base de datos: " + baseDatos);

        } catch (Exception e) {
            System.err.println("ERROR DE CONEXION HIBERNATE");
            e.printStackTrace();

        } finally {
            HibernateUtil.cerrar();
        }
    }
}
