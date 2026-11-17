package ni.edu.uam.nautaticket.service;

/** Identidad emitida por AuthService tras verificar la contraseña. */
public final class SesionAutenticada {
    private final int usuarioId;
    private volatile boolean activa = true;

    SesionAutenticada(int usuarioId) {
        if (usuarioId <= 0) throw new IllegalArgumentException("Usuario inválido.");
        this.usuarioId = usuarioId;
    }

    public int usuarioId() {
        if (!activa) throw new SecurityException("La sesión ha finalizado.");
        return usuarioId;
    }

    public void cerrar() {
        activa = false;
    }
}
