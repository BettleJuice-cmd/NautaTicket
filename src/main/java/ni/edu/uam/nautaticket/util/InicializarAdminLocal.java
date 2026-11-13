package ni.edu.uam.nautaticket.util;

import ni.edu.uam.nautaticket.dao.DepartamentoDao;
import ni.edu.uam.nautaticket.dao.RolDao;
import ni.edu.uam.nautaticket.dao.UsuarioDao;
import ni.edu.uam.nautaticket.model.Departamento;
import ni.edu.uam.nautaticket.model.Rol;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.AuthService;

import java.util.List;

public class InicializarAdminLocal {

    public static void main(String[] args) {

        try {
            String correo = System.getenv("ADMIN_EMAIL");
            String password = System.getenv("ADMIN_PASSWORD");

            if (correo == null || correo.isBlank()
                    || password == null || password.length() < 12) {
                throw new IllegalStateException(
                        "Configura ADMIN_EMAIL y ADMIN_PASSWORD " +
                                "con una contraseña de al menos 12 caracteres"
                );
            }

            UsuarioDao usuarioDao = new UsuarioDao();

            boolean existeAdmin = usuarioDao.listar().stream()
                    .anyMatch(u -> u.isActivo()
                            && u.getRol() != null
                            && "ADMIN".equalsIgnoreCase(
                            u.getRol().getNombre()));

            if (existeAdmin) {
                System.out.println(
                        "Ya existe un administrador activo"
                );
                return;
            }

            Rol rol = new RolDao().buscarPorNombre("ADMIN");

            List<Departamento> departamentos =
                    new DepartamentoDao().listar();

            if (rol == null || departamentos.isEmpty()) {
                throw new IllegalStateException(
                        "Debe existir el rol ADMIN y un departamento"
                );
            }

            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setApellido("NautaTicket");
            admin.setCorreo(correo);
            admin.setRol(rol);
            admin.setDepartamento(departamentos.get(0));

            new AuthService().registrar(admin, password);

            System.out.println(
                    "ADMINISTRADOR REGISTRADO CORRECTAMENTE"
            );
            System.out.println("Correo: " + correo);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HibernateUtil.cerrar();
        }
    }
}
