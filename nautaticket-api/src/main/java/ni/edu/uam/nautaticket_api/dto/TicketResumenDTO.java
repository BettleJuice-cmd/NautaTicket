package ni.edu.uam.nautaticket_api.dto;

import java.time.LocalDateTime;

public record TicketResumenDTO(
        Integer idTicket,
        String titulo,
        String categoria,
        String estado,
        LocalDateTime fechaCreacion
) {
}
