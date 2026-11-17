package ni.edu.uam.nautaticket_api;

import ni.edu.uam.nautaticket_api.controller.DepartamentoController;
import ni.edu.uam.nautaticket_api.controller.EstadisticaController;
import ni.edu.uam.nautaticket_api.controller.TicketController;
import ni.edu.uam.nautaticket_api.security.SecurityConfig;
import ni.edu.uam.nautaticket_api.service.EstadisticaService;
import ni.edu.uam.nautaticket_api.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@WebMvcTest({TicketController.class, DepartamentoController.class, EstadisticaController.class})
@Import({SecurityConfig.class, AutorizacionHttpTest.UsuariosPrueba.class})
class AutorizacionHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean TicketService tickets;
    @MockitoBean EstadisticaService estadisticas;
    @MockitoBean JdbcTemplate jdbc;

    @TestConfiguration
    static class UsuariosPrueba {
        @Bean UserDetailsService usuariosPrueba(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin-prueba").password(encoder.encode("clave-prueba"))
                            .roles("ADMIN").build(),
                    User.withUsername("tecnico-prueba").password(encoder.encode("clave-prueba"))
                            .roles("TECNICO").build(),
                    User.withUsername("empleado-prueba").password(encoder.encode("clave-prueba"))
                            .roles("EMPLEADO").build());
        }
    }

    @Test void sinAutenticarNoConsultaTickets() throws Exception {
        mvc.perform(get("/api/v1/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void empleadoNoConsultaListadoNiEstadisticasPeroConservaDepartamentos() throws Exception {
        mvc.perform(get("/api/v1/tickets").with(httpBasic("empleado-prueba", "clave-prueba")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/estadisticas").with(httpBasic("empleado-prueba", "clave-prueba")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tickets, estadisticas);
        doReturn(List.of()).when(jdbc).query(anyString(), any(RowMapper.class));
        mvc.perform(get("/api/v1/departamentos").with(httpBasic("empleado-prueba", "clave-prueba")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void adminConservaListadoYEstadisticas() throws Exception {
        doReturn(List.of()).when(tickets).listarTickets(any());
        doReturn(List.of()).when(estadisticas).obtenerTotalesPorEstado();
        mvc.perform(get("/api/v1/tickets").with(httpBasic("admin-prueba", "clave-prueba")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/v1/estadisticas").with(httpBasic("admin-prueba", "clave-prueba")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void tecnicoConsultaListadoSinPrivilegiosAdministrativos() throws Exception {
        doReturn(List.of()).when(tickets).listarTickets(any());
        mvc.perform(get("/api/v1/tickets").with(httpBasic("tecnico-prueba", "clave-prueba")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/v1/estadisticas").with(httpBasic("tecnico-prueba", "clave-prueba")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/diagnostico/db").with(httpBasic("tecnico-prueba", "clave-prueba")))
                .andExpect(status().isForbidden());
    }
}
