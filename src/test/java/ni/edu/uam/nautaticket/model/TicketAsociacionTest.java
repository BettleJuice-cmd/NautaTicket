package ni.edu.uam.nautaticket.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketAsociacionTest {

    @Test
    void mantieneAmbosExtremosYNoExponeColeccionMutable() {
        Ticket ticket = new Ticket();
        AsignacionTicket asignacion = new AsignacionTicket();

        ticket.agregarAsignacion(asignacion);
        ticket.agregarAsignacion(asignacion);
        assertSame(ticket, asignacion.getTicket());
        assertEquals(1, ticket.getAsignaciones().size());
        assertThrows(UnsupportedOperationException.class,
                () -> ticket.getAsignaciones().clear());

        ticket.retirarAsignacion(asignacion);
        assertNull(asignacion.getTicket());
        assertTrue(ticket.getAsignaciones().isEmpty());
    }

    @Test
    void protegeAsignacionesHistoricasYRechazaOtroTicket() {
        Ticket ticket = new Ticket();
        Ticket otro = new Ticket();
        AsignacionTicket asignacion = new AsignacionTicket();
        ticket.agregarAsignacion(asignacion);

        assertThrows(IllegalArgumentException.class,
                () -> otro.agregarAsignacion(asignacion));
        asignacion.setIdAsignacion(42);
        assertThrows(IllegalStateException.class,
                () -> ticket.retirarAsignacion(asignacion));
        assertSame(ticket, asignacion.getTicket());
        assertEquals(1, ticket.getAsignaciones().size());
    }
}
