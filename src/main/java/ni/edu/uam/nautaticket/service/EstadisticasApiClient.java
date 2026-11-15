package ni.edu.uam.nautaticket.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ni.edu.uam.nautaticket.dto.EstadisticaApiDTO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public class EstadisticasApiClient {

    private static final String URL =
            "http://localhost:8081/api/v1/estadisticas";

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<EstadisticaApiDTO> consultar(
            String correo,
            String password
    ) throws IOException, InterruptedException {

        if (correo == null || correo.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes ingresar el correo y la contraseña."
            );
        }

        String credenciales = correo + ":" + password;

        String autorizacion = Base64.getEncoder()
                .encodeToString(
                        credenciales.getBytes(StandardCharsets.UTF_8)
                );

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + autorizacion)
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(
                solicitud,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        return switch (respuesta.statusCode()) {
            case 200 -> objectMapper.readValue(
                    respuesta.body(),
                    new TypeReference<List<EstadisticaApiDTO>>() {}
            );

            case 401 -> throw new IOException(
                    "Correo o contraseña incorrectos."
            );

            case 403 -> throw new IOException(
                    "No tienes permisos para consultar las estadísticas."
            );

            default -> throw new IOException(
                    "La API respondió con el código HTTP "
                            + respuesta.statusCode()
            );
        };
    }
}
