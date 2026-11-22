package ni.edu.uam.nautaticket.service;

import ni.edu.uam.nautaticket.model.Usuario;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Objects;

/** Duraciones medidas desde la asignación hasta la resolución del ticket. */
public class TiempoResolucionService {
    private final SessionFactory sessionFactory;
    private final int administradorId;

    public record ResumenTecnico(int id, String nombre, long ultimoSegundos,
                                 double promedioSegundos, long ticketsResueltos) { }

    public TiempoResolucionService(SessionFactory sessionFactory, int administradorId) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
        this.administradorId = administradorId;
    }

    public List<ResumenTecnico> resumen() {
        try (Session session = sessionFactory.openSession()) {
            Usuario usuario = session.get(Usuario.class, administradorId);
            if (usuario == null || !usuario.isActivo() || usuario.getRol() == null
                    || !"ADMIN".equalsIgnoreCase(usuario.getRol().getNombre())) {
                throw new SecurityException("Solo el administrador puede consultar estos tiempos.");
            }

            return session.createNativeQuery("""
                    WITH asignaciones_resueltas AS (
                        SELECT a.id_tecnico, a.id_ticket, a.fecha_fin,
                               GREATEST(0, EXTRACT(EPOCH FROM
                                   (a.fecha_fin - a.fecha_asignacion))) AS segundos,
                               ROW_NUMBER() OVER (
                                   PARTITION BY a.id_ticket
                                   ORDER BY a.fecha_fin DESC, a.id_asignacion DESC
                               ) AS asignacion_ticket
                        FROM asignaciones_ticket a
                        JOIN tickets t ON t.id_ticket = a.id_ticket
                        WHERE a.fecha_fin IS NOT NULL
                          AND t.fecha_resolucion IS NOT NULL
                          AND a.id_tecnico = t.id_tecnico
                          AND a.fecha_fin >= a.fecha_asignacion
                    ), duraciones AS (
                        SELECT id_tecnico, id_ticket, fecha_fin, segundos,
                               ROW_NUMBER() OVER (
                                   PARTITION BY id_tecnico
                                   ORDER BY fecha_fin DESC, id_ticket DESC
                               ) AS orden
                        FROM asignaciones_resueltas
                        WHERE asignacion_ticket = 1
                    )
                    SELECT u.id_usuario, u.nombre || ' ' || u.apellido,
                           MAX(CASE WHEN d.orden = 1 THEN d.segundos END),
                           AVG(d.segundos), COUNT(*)
                    FROM duraciones d
                    JOIN usuarios u ON u.id_usuario = d.id_tecnico
                    GROUP BY u.id_usuario, u.nombre, u.apellido
                    ORDER BY u.nombre, u.apellido, u.id_usuario
                    """, Object[].class).getResultList().stream()
                    .map(f -> new ResumenTecnico(
                            ((Number) f[0]).intValue(), (String) f[1],
                            ((Number) f[2]).longValue(),
                            ((Number) f[3]).doubleValue(),
                            ((Number) f[4]).longValue()))
                    .toList();
        }
    }

    public static String formatear(long segundos) {
        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;
        long resto = segundos % 60;
        return horas > 0 ? "%d h %02d min %02d s".formatted(horas, minutos, resto)
                : "%d min %02d s".formatted(minutos, resto);
    }
}
