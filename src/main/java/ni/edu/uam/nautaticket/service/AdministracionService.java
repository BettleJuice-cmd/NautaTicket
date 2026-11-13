package ni.edu.uam.nautaticket.service;

import ni.edu.uam.nautaticket.dao.*;
import ni.edu.uam.nautaticket.model.*;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Locale;

public class AdministracionService {

    private static final int LIMITE_NOMBRE_DEPARTAMENTO = 100;

    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final DepartamentoDao departamentoDao = new DepartamentoDao();
    private final RolDao rolDao = new RolDao();
    private final AuthService authService = new AuthService();

    private final Integer administradorId;

    public AdministracionService(Usuario administrador) {
        if (administrador == null || administrador.getId() == null) {
            throw new SecurityException("Sesión inválida");
        }

        this.administradorId = administrador.getId();
        verificarAdministrador();
    }

    private void verificarAdministrador() {
        Usuario administrador = usuarioDao.buscarPorId(administradorId);

        if (administrador == null
                || !administrador.isActivo()
                || administrador.getRol() == null
                || !"ADMIN".equalsIgnoreCase(
                administrador.getRol().getNombre())) {
            throw new SecurityException(
                    "No tiene permisos de administrador"
            );
        }
    }

    public List<Usuario> listarUsuarios() {
        verificarAdministrador();
        return usuarioDao.listar();
    }

    public List<Rol> listarRoles() {
        verificarAdministrador();
        return rolDao.listar();
    }

    public List<Departamento> listarDepartamentos() {
        verificarAdministrador();
        return departamentoDao.listar();
    }

    public void registrarUsuario(
            String nombre,
            String apellido,
            String correo,
            String password,
            Rol rol,
            Departamento departamento) {

        verificarAdministrador();
        validarDatos(nombre, apellido, correo, rol, departamento);

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setApellido(apellido.trim());
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setDepartamento(departamento);

        authService.registrar(usuario, password);
    }

    public void actualizarUsuario(
            Integer id,
            String nombre,
            String apellido,
            String correo,
            String password,
            Rol rol,
            Departamento departamento) {

        verificarAdministrador();
        validarDatos(nombre, apellido, correo, rol, departamento);

        Usuario usuario = usuarioDao.buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        if (administradorId.equals(id)
                && !usuario.getRol().getId().equals(rol.getId())) {
            throw new IllegalArgumentException(
                    "No puedes cambiar tu propio rol"
            );
        }

        String correoNormalizado =
                correo.trim().toLowerCase(Locale.ROOT);

        Usuario existente = usuarioDao.buscarPorCorreo(correoNormalizado);

        if (existente != null && !existente.getId().equals(id)) {
            throw new IllegalArgumentException(
                    "El correo ya está registrado"
            );
        }

        usuario.setNombre(nombre.trim());
        usuario.setApellido(apellido.trim());
        usuario.setCorreo(correoNormalizado);
        usuario.setRol(rol);
        usuario.setDepartamento(departamento);

        if (password != null && !password.isBlank()) {
            if (password.length() < 8) {
                throw new IllegalArgumentException(
                        "La contraseña debe tener al menos 8 caracteres"
                );
            }

            usuario.setPasswordHash(
                    BCrypt.hashpw(password, BCrypt.gensalt())
            );
        }

        usuarioDao.actualizar(usuario);
    }

    public void desactivarUsuario(Integer id) {
        verificarAdministrador();

        if (id == null) {
            throw new IllegalArgumentException(
                    "Selecciona un usuario"
            );
        }

        if (administradorId.equals(id)) {
            throw new IllegalArgumentException(
                    "No puedes desactivar tu propia cuenta"
            );
        }

        usuarioDao.desactivar(id);
    }

    public void crearDepartamento(String nombre, String descripcion) {
        verificarAdministrador();

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del departamento es obligatorio"
            );
        }

        String nombreNormalizado = nombre.trim();
        if (nombreNormalizado.codePointCount(0, nombreNormalizado.length()) > LIMITE_NOMBRE_DEPARTAMENTO) {
            throw new IllegalArgumentException(
                    "El nombre del departamento no puede superar " + LIMITE_NOMBRE_DEPARTAMENTO + " caracteres"
            );
        }

        departamentoDao.guardar(
                new Departamento(
                        nombreNormalizado,
                        descripcion == null ? "" : descripcion.trim()
                )
        );
    }

    private void validarDatos(
            String nombre,
            String apellido,
            String correo,
            Rol rol,
            Departamento departamento) {

        if (nombre == null || nombre.isBlank()
                || apellido == null || apellido.isBlank()) {
            throw new IllegalArgumentException(
                    "Nombre y apellido son obligatorios"
            );
        }

        if (correo == null ||
                !correo.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException(
                    "Ingresa un correo válido"
            );
        }

        if (rol == null || departamento == null) {
            throw new IllegalArgumentException(
                    "Selecciona un rol y un departamento"
            );
        }
    }
}
