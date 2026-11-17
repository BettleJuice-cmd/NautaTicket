
package ni.edu.uam.nautaticket.service;

import ni.edu.uam.nautaticket.dao.AsignacionTicketDao;
import ni.edu.uam.nautaticket.dao.AsignacionTicketDao.TicketFila;
import ni.edu.uam.nautaticket.model.AsignacionTicket;
import ni.edu.uam.nautaticket.model.Ticket;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class AtencionService {

    public static final int MAX_TICKETS_ACTIVOS = 3;

    private final SessionFactory sessionFactory;
    private final AsignacionTicketDao dao;
    private final HistorialService historialService;

    public AtencionService() {
        this(HibernateUtil.getSessionFactory());
    }

    public AtencionService(SessionFactory sessionFactory) {

        if (sessionFactory == null) {
            throw new IllegalArgumentException(
                    "La conexión Hibernate es obligatoria."
            );
        }

        this.sessionFactory = sessionFactory;
        this.dao = new AsignacionTicketDao(sessionFactory);
        this.historialService = new HistorialService(sessionFactory);
    }

    public List<TicketFila> listarPendientes() {
        return dao.listarPendientes();
    }

    public List<TicketFila> listarMisTickets(int idTecnico) {

        verificarTecnicoActivo(idTecnico);

        return dao.listarMisTickets(idTecnico);
    }

    public int contarTicketsActivos(int idTecnico) {

        verificarTecnicoActivo(idTecnico);

        return Math.toIntExact(
                dao.contarActivas(idTecnico)
        );
    }

    public int tomarSiguienteTicket(int idTecnico) {

        return sessionFactory.fromTransaction(session -> {

            Usuario tecnico = buscarTecnicoActivo(
                    session,
                    idTecnico
            );

            // Conservamos el bloqueo transaccional FIFO.
            // También serializa las tomas que pueden
            // modificar la capacidad de cada técnico.
            dao.bloquearColaParaAsignar(session);

            // La capacidad se verifica DESPUÉS de
            // adquirir el bloqueo y DENTRO de la
            // misma transacción que asigna el ticket.
            long activos = dao.contarActivas(
                    session,
                    idTecnico
            );

            if (activos >= MAX_TICKETS_ACTIVOS) {

                throw new IllegalStateException(
                        "Has alcanzado el máximo de "
                                + MAX_TICKETS_ACTIVOS
                                + " tickets activos. "
                                + "Resuelve uno antes de tomar otro."
                );
            }

            Integer idTicket = dao.buscarSiguienteFIFO(
                    session
            );

            if (idTicket == null) {

                throw new IllegalStateException(
                        "No hay tickets pendientes disponibles."
                );
            }

            int actualizados =
                    session.createNativeMutationQuery("""
                            UPDATE tickets
                            SET id_tecnico = :tecnico,
                                fecha_asignacion = CURRENT_TIMESTAMP
                            WHERE id_ticket = :ticket
                              AND id_tecnico IS NULL
                            """)
                            .setParameter(
                                    "tecnico",
                                    idTecnico
                            )
                            .setParameter(
                                    "ticket",
                                    idTicket
                            )
                            .executeUpdate();

            if (actualizados != 1) {

                throw new IllegalStateException(
                        "El ticket ya fue asignado."
                );
            }

            Ticket ticket = session.getReference(
                    Ticket.class,
                    idTicket
            );

            session.persist(
                    new AsignacionTicket(
                            ticket,
                            tecnico
                    )
            );

            historialService.registrarCambioTecnico(
                    session,
                    ticket,
                    tecnico,
                    null,
                    idTecnico
            );

            return idTicket;
        });
    }

    public void iniciarAtencion(
            int idTicket,
            int idTecnico
    ) {

        sessionFactory.inTransaction(session -> {

            Usuario tecnico = buscarTecnicoActivo(
                    session,
                    idTecnico
            );

            int actualizados =
                    session.createNativeMutationQuery("""
                            UPDATE tickets t
                            SET id_estado = destino.id_estado
                            FROM estados_ticket origen,
                                 estados_ticket destino
                            WHERE t.id_ticket = :ticket
                              AND t.id_tecnico = :tecnico
                              AND t.id_estado = origen.id_estado
                              AND origen.nombre = 'PENDIENTE'
                              AND destino.nombre = 'EN_PROCESO'
                              AND EXISTS (
                                  SELECT 1
                                  FROM asignaciones_ticket a
                                  WHERE a.id_ticket = :ticket
                                    AND a.id_tecnico = :tecnico
                                    AND a.fecha_fin IS NULL
                              )
                            """)
                            .setParameter(
                                    "ticket",
                                    idTicket
                            )
                            .setParameter(
                                    "tecnico",
                                    idTecnico
                            )
                            .executeUpdate();

            if (actualizados != 1) {

                throw new IllegalStateException(
                        "No se puede iniciar la atención del ticket."
                );
            }

            historialService.registrarCambioEstado(
                    session,
                    session.getReference(
                            Ticket.class,
                            idTicket
                    ),
                    tecnico,
                    "PENDIENTE",
                    "EN_PROCESO",
                    "Inicio de atención del ticket."
            );
        });
    }

    public void resolverTicket(
            int idTicket,
            int idTecnico,
            String solucion
    ) {

        if (solucion == null || solucion.isBlank()) {

            throw new IllegalArgumentException(
                    "Debes escribir la solución del ticket."
            );
        }

        String solucionRegistrada = solucion.trim();

        sessionFactory.inTransaction(session -> {

            Usuario tecnico = buscarTecnicoActivo(
                    session,
                    idTecnico
            );

            int actualizados =
                    session.createNativeMutationQuery("""
                            UPDATE tickets t
                            SET id_estado = destino.id_estado,
                                solucion = :solucion,
                                fecha_resolucion = CURRENT_TIMESTAMP
                            FROM estados_ticket origen,
                                 estados_ticket destino
                            WHERE t.id_ticket = :ticket
                              AND t.id_tecnico = :tecnico
                              AND t.id_estado = origen.id_estado
                              AND origen.nombre = 'EN_PROCESO'
                              AND destino.nombre = 'RESUELTO'
                            """)
                            .setParameter(
                                    "solucion",
                                    solucionRegistrada
                            )
                            .setParameter(
                                    "ticket",
                                    idTicket
                            )
                            .setParameter(
                                    "tecnico",
                                    idTecnico
                            )
                            .executeUpdate();

            if (actualizados != 1) {

                throw new IllegalStateException(
                        "El ticket no está en proceso "
                                + "o no pertenece al técnico."
                );
            }

            int asignacionesCerradas =
                    session.createNativeMutationQuery("""
                            UPDATE asignaciones_ticket
                            SET fecha_fin = CURRENT_TIMESTAMP
                            WHERE id_ticket = :ticket
                              AND id_tecnico = :tecnico
                              AND fecha_fin IS NULL
                            """)
                            .setParameter(
                                    "ticket",
                                    idTicket
                            )
                            .setParameter(
                                    "tecnico",
                                    idTecnico
                            )
                            .executeUpdate();

            if (asignacionesCerradas != 1) {

                throw new IllegalStateException(
                        "La resolución requiere exactamente "
                                + "una asignación activa del técnico."
                );
            }

            historialService.registrarCambioEstado(
                    session,
                    session.getReference(
                            Ticket.class,
                            idTicket
                    ),
                    tecnico,
                    "EN_PROCESO",
                    "RESUELTO",
                    "Ticket resuelto: " + solucionRegistrada
            );
        });
    }

    private void verificarTecnicoActivo(
            int idTecnico
    ) {

        try (Session session =
                     sessionFactory.openSession()) {

            buscarTecnicoActivo(
                    session,
                    idTecnico
            );
        }
    }

    private Usuario buscarTecnicoActivo(
            Session session,
            int idTecnico
    ) {

        if (idTecnico <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del técnico no es válido."
            );
        }

        return session.createSelectionQuery("""
                FROM Usuario u
                WHERE u.id = :tecnico
                  AND u.activo = true
                  AND u.rol.nombre = 'TECNICO'
                """, Usuario.class)
                .setParameter(
                        "tecnico",
                        idTecnico
                )
                .uniqueResultOptional()
                .orElseThrow(() ->
                        new SecurityException(
                                "Solo un técnico TI activo "
                                        + "puede atender tickets."
                        )
                );
    }
}
