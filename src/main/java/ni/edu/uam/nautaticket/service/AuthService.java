package ni.edu.uam.nautaticket.service;

import ni.edu.uam.nautaticket.dao.UsuarioDao;
import ni.edu.uam.nautaticket.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Locale;

public class AuthService {

    private final UsuarioDao usuarioDao = new UsuarioDao();

    public Usuario iniciarSesion(String correo, String password) {

        if (correo == null || correo.isBlank()
                || password == null || password.isBlank()) {
            return null;
        }

        String correoNormalizado =
                correo.trim().toLowerCase(Locale.ROOT);

        Usuario usuario =
                usuarioDao.buscarPorCorreo(correoNormalizado);

        if (usuario == null || !usuario.isActivo()) {
            return null;
        }

        String hash = usuario.getPasswordHash();

        if (hash == null || !hash.startsWith("$2")) {
            return null;
        }

        try {
            if (BCrypt.checkpw(password, hash)) {
                return usuario;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }

        return null;
    }

    public void registrar(Usuario usuario, String password) {

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "Debe proporcionar un usuario"
            );
        }

        if (usuario.getNombre() == null
                || usuario.getNombre().isBlank()
                || usuario.getApellido() == null
                || usuario.getApellido().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre y apellido son obligatorios"
            );
        }

        if (usuario.getRol() == null) {
            throw new IllegalArgumentException(
                    "Debe asignar un rol"
            );
        }

        if (usuario.getCorreo() == null
                || usuario.getCorreo().isBlank()) {
            throw new IllegalArgumentException(
                    "El correo es obligatorio"
            );
        }

        String correo = usuario.getCorreo()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException(
                    "El correo no es válido"
            );
        }

        if (usuarioDao.buscarPorCorreo(correo) != null) {
            throw new IllegalArgumentException(
                    "El correo ya está registrado"
            );
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres"
            );
        }

        usuario.setCorreo(correo);
        usuario.setActivo(true);
        usuario.setPasswordHash(
                BCrypt.hashpw(password, BCrypt.gensalt())
        );

        usuarioDao.guardar(usuario);
    }
}
