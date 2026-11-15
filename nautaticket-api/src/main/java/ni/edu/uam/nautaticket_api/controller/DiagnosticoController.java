package ni.edu.uam.nautaticket_api.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/diagnostico")
public class DiagnosticoController {

    private final JdbcTemplate jdbcTemplate;

    public DiagnosticoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/db")
    public Map<String, String> comprobarBaseDatos() {
        String baseDatos = jdbcTemplate.queryForObject(
                "SELECT current_database()",
                String.class
        );

        return Map.of(
                "estado", "CONECTADO",
                "baseDatos", baseDatos
        );
    }
}
