package ni.edu.uam.nautaticket_api.service;

import ni.edu.uam.nautaticket_api.dto.TicketResumenDTO;
import ni.edu.uam.nautaticket_api.repository.TicketRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<TicketResumenDTO> listarTickets(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Usuario no autenticado"
            );
        }

        String correo = authentication.getName();

        if (tieneRol(authentication, "ROLE_ADMIN")) {
            return ticketRepository.listarTodos();
        }

        if (tieneRol(authentication, "ROLE_TECNICO")) {
            return ticketRepository.listarPorTecnico(correo);
        }

        if (tieneRol(authentication, "ROLE_EMPLEADO")) {
            return ticketRepository.listarPorSolicitante(correo);
        }

        throw new AccessDeniedException(
                "No tiene permisos para consultar tickets"
        );
    }

    private boolean tieneRol(
            Authentication authentication,
            String rol) {

        return authentication.getAuthorities()
                .stream()
                .anyMatch(autoridad ->
                        autoridad.getAuthority().equals(rol)
                );
    }
}
