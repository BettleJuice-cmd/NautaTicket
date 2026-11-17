package ni.edu.uam.nautaticket_api.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class NautaUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public NautaUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String correo)
            throws UsernameNotFoundException {

        String sql = """
                SELECT
                    u.correo,
                    u.password_hash,
                    u.activo,
                    r.nombre AS rol
                FROM usuarios u
                INNER JOIN roles r
                    ON u.id_rol = r.id_rol
                WHERE LOWER(u.correo) = LOWER(?)
                """;

        return jdbcTemplate.query(
                sql,
                ps -> ps.setString(1, correo),
                rs -> {
                    if (!rs.next()) {
                        throw new UsernameNotFoundException(
                                "Usuario no encontrado"
                        );
                    }

                    return User.withUsername(rs.getString("correo"))
                            .password(rs.getString("password_hash"))
                            .roles(rs.getString("rol").toUpperCase())
                            .disabled(!rs.getBoolean("activo"))
                            .build();
                }
        );
    }
}
