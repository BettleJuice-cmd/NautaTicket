
package ni.edu.uam.nautaticket.service;

import ni.edu.uam.nautaticket.dao.AsignacionTicketDao;
import ni.edu.uam.nautaticket.dao.AsignacionTicketDao.TicketFila;
import ni.edu.uam.nautaticket.model.AsignacionTicket;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public class AtencionService {

    private final SessionFactory sessionFactory;
    private final AsignacionTicketDao dao;

    public AtencionService() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
        this.dao = new AsignacionTicketDao();
    }

    public List<TicketFila> listarPendientes() {
        return dao.listarPendientes();
    }

    public List<TicketFila> listarMisTickets(int idTecnico) {
        return dao.listarMisTickets(idTecnico);
    }

    public int tomarSiguienteTicket(int idTecnico) {
        return ejecutarTransaccion(session -> {
            validarTecnico(session, idTecnico);

            Integer idTicket = dao.buscarSiguienteFIFO(session);

            if (idTicket == null) {
                throw new IllegalStateException(
                        "No hay tickets pendientes disponibles.");
            }

            int actualizados = session.createNativeMutationQuery("""
                    UPDATE tickets
                    SET id_tecnico = :tecnico,
                        fecha_asignacion = CURRENT_TIMESTAMP
                    WHERE id_ticket = :ticket
                      AND id_tecnico IS NULL
                    """)
                    .setParameter("tecnico", idTecnico)
                    .setParameter("ticket", idTicket)
                    .executeUpdate();

            if (actualizados != 1) {
                throw new IllegalStateException(
                        "El ticket ya fue asignado.");
            }

            AsignacionTicket asignacion =
                    new AsignacionTicket(idTicket, idTecnico);

            session.persist(asignacion);

            registrarHistorial(
                    session,
                    idTicket,
                    idTecnico,
                    "PENDIENTE",
                    "PENDIENTE",
                    "Ticket asignado al tecnico."
            );

            return idTicket;
        });
    }

    public void iniciarAtencion(int idTicket, int idTecnico) {
        ejecutarTransaccion(session -> {
            validarTecnico(session, idTecnico);

            int actualizados = session.createNativeMutationQuery("""
                    UPDATE tickets
                    SET id_estado = (
                            SELECT id_estado
                            FROM estados_ticket
                            WHERE nombre = 'EN_PROCESO'
                        ),
                        estado = 'EN_PROCESO'
                    WHERE id_ticket = :ticket
                      AND id_tecnico = :tecnico
                      AND estado = 'PENDIENTE'
                      AND EXISTS (
                          SELECT 1
                          FROM asignaciones_ticket a
                          WHERE a.id_ticket = :ticket
                            AND a.id_tecnico = :tecnico
                            AND a.fecha_fin IS NULL
                      )
                    """)
                    .setParameter("ticket", idTicket)
                    .setParameter("tecnico", idTecnico)
                    .executeUpdate();

            if (actualizados != 1) {
                throw new IllegalStateException(
                        "No se puede iniciar la atencion del ticket.");
            }

            registrarHistorial(
                    session,
                    idTicket,
                    idTecnico,
                    "PENDIENTE",
                    "EN_PROCESO",
                    "Inicio de atencion del ticket."
            );

            return null;
        });
    }

    public void resolverTicket(
            int idTicket,
            int idTecnico,
            String solucion
    ) {
        if (solucion == null || solucion.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes escribir la solucion del ticket.");
        }

        ejecutarTransaccion(session -> {
            validarTecnico(session, idTecnico);

            int actualizados = session.createNativeMutationQuery("""
                    UPDATE tickets
                    SET id_estado = (
                            SELECT id_estado
                            FROM estados_ticket
                            WHERE nombre = 'RESUELTO'
                        ),
                        estado = 'RESUELTO',
                        solucion = :solucion,
                        fecha_resolucion = CURRENT_TIMESTAMP
                    WHERE id_ticket = :ticket
                      AND id_tecnico = :tecnico
                      AND estado = 'EN_PROCESO'
                      AND EXISTS (
                          SELECT 1
                          FROM asignaciones_ticket a
                          WHERE a.id_ticket = :ticket
                            AND a.id_tecnico = :tecnico
                            AND a.fecha_fin IS NULL
                      )
                    """)
                    .setParameter("solucion", solucion.trim())
                    .setParameter("ticket", idTicket)
                    .setParameter("tecnico", idTecnico)
                    .executeUpdate();

            if (actualizados != 1) {
                throw new IllegalStateException(
                        "El ticket no esta en proceso o no pertenece al tecnico.");
            }

            session.createNativeMutationQuery("""
                    UPDATE asignaciones_ticket
                    SET fecha_fin = CURRENT_TIMESTAMP
                    WHERE id_ticket = :ticket
                      AND id_tecnico = :tecnico
                      AND fecha_fin IS NULL
                    """)
                    .setParameter("ticket", idTicket)
                    .setParameter("tecnico", idTecnico)
                    .executeUpdate();

            registrarHistorial(
                    session,
                    idTicket,
                    idTecnico,
                    "EN_PROCESO",
                    "RESUELTO",
                    "Ticket resuelto: " + solucion.trim()
            );

            return null;
        });
    }

    private void validarTecnico(Session session, int idTecnico) {
        Object resultado = session.createNativeQuery("""
                SELECT COUNT(*)
                FROM usuarios u
                JOIN roles r ON r.id_rol = u.id_rol
                WHERE u.id_usuario = :tecnico
                  AND u.activo = TRUE
                  AND r.nombre IN ('TECNICO', 'ADMIN')
                """)
                .setParameter("tecnico", idTecnico)
                .getSingleResult();

        if (((Number) resultado).longValue() == 0) {
            throw new IllegalArgumentException(
                    "El usuario no es un tecnico activo.");
        }
    }

    private void registrarHistorial(
            Session session,
            int idTicket,
            int idTecnico,
            String estadoAnterior,
            String estadoNuevo,
            String comentario
    ) {
        session.createNativeMutationQuery("""
                INSERT INTO historial_tickets
                (id_ticket, id_usuario, estado_anterior,
                 estado_nuevo, comentario)
                VALUES
                (:ticket, :tecnico, :anterior, :nuevo, :comentario)
                """)
                .setParameter("ticket", idTicket)
                .setParameter("tecnico", idTecnico)
                .setParameter("anterior", estadoAnterior)
                .setParameter("nuevo", estadoNuevo)
                .setParameter("comentario", comentario)
                .executeUpdate();
    }

    private <T> T ejecutarTransaccion(
            Function<Session, T> operacion
    ) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaccion = session.beginTransaction();

            try {
                T resultado = operacion.apply(session);
                transaccion.commit();
                return resultado;

            } catch (RuntimeException e) {
                if (transaccion.isActive()) {
                    transaccion.rollback();
                }
                throw e;
            }
        }
    }
}
