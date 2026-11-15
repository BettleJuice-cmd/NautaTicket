package ni.edu.uam.nautaticket_api.repository;

import ni.edu.uam.nautaticket_api.dto.TicketResumenDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TicketRepository {

    private final JdbcTemplate jdbcTemplate;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String CONSULTA_BASE = """
            SELECT
                t.id_ticket,
                t.titulo,
                c.nombre AS categoria,
                e.nombre AS estado,
                t.fecha_creacion
            FROM tickets t
            INNER JOIN categorias c
                ON t.id_categoria = c.id_categoria
            INNER JOIN estados_ticket e
                ON t.id_estado = e.id_estado
            """;

    private static final String ORDEN = """
            ORDER BY t.fecha_creacion DESC, t.id_ticket DESC
            """;

    private final org.springframework.jdbc.core.RowMapper<TicketResumenDTO>
            ticketMapper = (rs, rowNum) ->
            new TicketResumenDTO(
                    rs.getInt("id_ticket"),
                    rs.getString("titulo"),
                    rs.getString("categoria"),
                    rs.getString("estado"),
                    rs.getTimestamp("fecha_creacion")
                            .toLocalDateTime()
            );

    public List<TicketResumenDTO> listarTodos() {
        String sql = CONSULTA_BASE + ORDEN;
        return jdbcTemplate.query(sql, ticketMapper);
    }

    public List<TicketResumenDTO> listarPorSolicitante(String correo) {
        String sql = CONSULTA_BASE + """
                INNER JOIN usuarios u
                    ON t.id_solicitante = u.id_usuario
                WHERE LOWER(u.correo) = LOWER(?)
                """ + ORDEN;

        return jdbcTemplate.query(sql, ticketMapper, correo);
    }

    public List<TicketResumenDTO> listarPorTecnico(String correo) {
        String sql = CONSULTA_BASE + """
                INNER JOIN usuarios u
                    ON t.id_tecnico = u.id_usuario
                WHERE LOWER(u.correo) = LOWER(?)
                """ + ORDEN;

        return jdbcTemplate.query(sql, ticketMapper, correo);
    }
}
