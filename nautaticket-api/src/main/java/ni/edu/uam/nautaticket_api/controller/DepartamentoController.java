package ni.edu.uam.nautaticket_api.controller;

import ni.edu.uam.nautaticket_api.dto.DepartamentoDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departamentos")
public class DepartamentoController {

    private final JdbcTemplate jdbcTemplate;

    public DepartamentoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<DepartamentoDTO> listarDepartamentos() {
        String sql = """
                SELECT id_departamento, nombre, descripcion
                FROM departamentos
                ORDER BY nombre
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new DepartamentoDTO(
                        rs.getInt("id_departamento"),
                        rs.getString("nombre"),
                        rs.getString("descripcion")
                )
        );
    }
}
