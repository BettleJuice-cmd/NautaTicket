package ni.edu.uam.nautaticket.service;

import org.hibernate.SessionFactory;

import java.util.Arrays;
import java.util.Objects;

/** Comprueba el rol vigente de una identidad establecida por el login Java. */
public final class AutorizacionService {
    private final SessionFactory sesiones;
    private final int usuarioId;

    public AutorizacionService(SessionFactory sesiones, Integer usuarioId) {
        this.sesiones = Objects.requireNonNull(sesiones, "La conexión es obligatoria.");
        if (usuarioId == null || usuarioId <= 0) {
            throw new SecurityException("Se necesita un usuario autenticado.");
        }
        this.usuarioId = usuarioId;
    }

    public String exigirRol(String... permitidos) {
        String rol = sesiones.fromSession(session -> session.createSelectionQuery("""
                SELECT u.rol.nombre FROM Usuario u
                WHERE u.id = :usuario AND u.activo = true
                """, String.class)
                .setParameter("usuario", usuarioId)
                .uniqueResult());
        if (rol == null || Arrays.stream(permitidos).noneMatch(rol::equals)) {
            throw new SecurityException("Tu cuenta no tiene permiso para esta operación.");
        }
        return rol;
    }
}
