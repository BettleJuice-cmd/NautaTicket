package ni.edu.uam.nautaticket_api.controller;

import ni.edu.uam.nautaticket_api.dto.TicketResumenDTO;
import ni.edu.uam.nautaticket_api.service.TicketService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<TicketResumenDTO> listarTickets(
            Authentication authentication) {

        return ticketService.listarTickets(authentication);
    }
}
