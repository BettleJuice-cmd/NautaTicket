
package ni.edu.uam.nautaticket.service;

import ni.edu.uam.nautaticket.dao.TicketDAO;
import ni.edu.uam.nautaticket.model.Categoria;
import ni.edu.uam.nautaticket.model.Ticket;
import org.hibernate.SessionFactory;

import java.util.List;

public class TicketService {

    private final SessionFactory sesiones;
    private final int usuarioId;
    private final AutorizacionService autorizacion;

    public TicketService(SessionFactory sesiones, Integer usuarioId) {
        this.autorizacion = new AutorizacionService(sesiones, usuarioId);
        this.sesiones = sesiones;
        this.usuarioId = usuarioId;
    }

    private TicketDAO daoAdministrador() {
        autorizacion.exigirRol("ADMIN");
        return new TicketDAO(sesiones);
    }

    public void crearTicket(String titulo, String descripcion,
                            Categoria categoria, Integer idSolicitante) {

        validarDatos(titulo, descripcion, categoria);

        autorizacion.exigirRol("EMPLEADO", "ADMIN");

        if (!Integer.valueOf(usuarioId).equals(idSolicitante)) {
            throw new SecurityException("El solicitante debe ser el usuario autenticado.");
        }

        Ticket ticket = new Ticket();
        ticket.setTitulo(titulo.trim());
        ticket.setDescripcion(descripcion.trim());
        ticket.setCategoria(categoria);
        new TicketDAO(sesiones, usuarioId).guardar(ticket, usuarioId);
    }

    public List<Ticket> listarTickets() {
        return daoAdministrador().listar();
    }

    public Ticket buscarTicket(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de ticket inválido.");
        }

        return daoAdministrador().buscarPorId(id);
    }

    public List<Ticket> buscarPorCategoria(Integer idCategoria) {
        if (idCategoria == null || idCategoria <= 0) {
            throw new IllegalArgumentException("Seleccione una categoría válida.");
        }

        return daoAdministrador().buscarPorCategoria(idCategoria);
    }

    public boolean actualizarTicket(Integer id, String titulo,
                                    String descripcion, Categoria categoria) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de ticket inválido.");
        }

        validarDatos(titulo, descripcion, categoria);

        return daoAdministrador().actualizarDatos(
                id,
                titulo.trim(),
                descripcion.trim(),
                categoria.getIdCategoria()
        );
    }

    public boolean eliminarTicket(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de ticket inválido.");
        }

        return daoAdministrador().eliminar(id);
    }

    private void validarDatos(String titulo, String descripcion,
                              Categoria categoria) {

        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio.");
        }

        if (titulo.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "El título no puede superar los 150 caracteres.");
        }

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria.");
        }

        if (categoria == null || categoria.getIdCategoria() == null) {
            throw new IllegalArgumentException("Seleccione una categoría.");
        }

        if (!categoria.isActiva()) {
            throw new IllegalArgumentException(
                    "La categoría seleccionada no está activa.");
        }
    }
}
