
package ni.edu.uam.nautaticket.service;

import ni.edu.uam.nautaticket.dao.TicketDAO;
import ni.edu.uam.nautaticket.model.Categoria;
import ni.edu.uam.nautaticket.model.Ticket;

import java.util.List;

public class TicketService {

    private final TicketDAO ticketDAO;

    public TicketService(TicketDAO ticketDAO) {
        this.ticketDAO = ticketDAO;
    }

    public void crearTicket(String titulo, String descripcion,
                            Categoria categoria, Integer idSolicitante) {

        validarDatos(titulo, descripcion, categoria);

        if (idSolicitante == null || idSolicitante <= 0) {
            throw new IllegalArgumentException("El solicitante es obligatorio.");
        }

        Ticket ticket = new Ticket();
        ticket.setTitulo(titulo.trim());
        ticket.setDescripcion(descripcion.trim());
        ticket.setCategoria(categoria);
        ticket.setIdSolicitante(idSolicitante);

        ticketDAO.guardar(ticket);
    }

    public List<Ticket> listarTickets() {
        return ticketDAO.listar();
    }

    public Ticket buscarTicket(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de ticket inválido.");
        }

        return ticketDAO.buscarPorId(id);
    }

    public List<Ticket> buscarPorCategoria(Integer idCategoria) {
        if (idCategoria == null || idCategoria <= 0) {
            throw new IllegalArgumentException("Seleccione una categoría válida.");
        }

        return ticketDAO.buscarPorCategoria(idCategoria);
    }

    public boolean actualizarTicket(Integer id, String titulo,
                                    String descripcion, Categoria categoria) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de ticket inválido.");
        }

        validarDatos(titulo, descripcion, categoria);

        return ticketDAO.actualizarDatos(
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

        return ticketDAO.eliminar(id);
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
