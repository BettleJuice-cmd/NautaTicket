package ni.edu.uam.nautaticket_api.repository;

import ni.edu.uam.nautaticket_api.dto.EstadisticaEstadoDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EstadisticaRepository {

    private final JdbcTemplate jdbcTemplate;

    public EstadisticaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EstadisticaEstadoDTO> contarPorEstado() {

        String sql = """
                SELECT
                    e.nombre AS estado,
                    COUNT(t.id_ticket) AS total
                FROM estados_ticket e
                LEFT JOIN tickets t
                    ON t.id_estado = e.id_estado
                GROUP BY e.id_estado, e.nombre
                ORDER BY e.id_estado
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new EstadisticaEstadoDTO(
                        rs.getString("estado"),
                        rs.getLong("total")
                )
        );
    }
}
