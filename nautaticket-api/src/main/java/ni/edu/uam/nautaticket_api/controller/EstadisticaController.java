package ni.edu.uam.nautaticket_api.controller;

import ni.edu.uam.nautaticket_api.dto.EstadisticaEstadoDTO;
import ni.edu.uam.nautaticket_api.service.EstadisticaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/estadisticas")
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    public EstadisticaController(
            EstadisticaService estadisticaService) {
        this.estadisticaService = estadisticaService;
    }

    @GetMapping
    public List<EstadisticaEstadoDTO> obtenerEstadisticas() {
        return estadisticaService.obtenerTotalesPorEstado();
    }
}
