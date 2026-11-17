package ni.edu.uam.nautaticket_api;

import ni.edu.uam.nautaticket_api.repository.TicketRepository;
import ni.edu.uam.nautaticket_api.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TicketServiceRolesTest {
    @Test void empleadoNoPuedeEludirFiltroHttpInvocandoServicio() {
        var repositorio = mock(TicketRepository.class);
        var usuario = new UsernamePasswordAuthenticationToken(
                "empleado@nauta.invalid", null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLEADO")));
        assertThrows(AccessDeniedException.class,
                () -> new TicketService(repositorio).listarTickets(usuario));
        verifyNoInteractions(repositorio);
    }
}
