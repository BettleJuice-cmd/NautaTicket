package ni.edu.uam.nautaticket_api.service;

import ni.edu.uam.nautaticket_api.dto.EstadisticaEstadoDTO;
import ni.edu.uam.nautaticket_api.repository.EstadisticaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadisticaService {

    private final EstadisticaRepository estadisticaRepository;

    public EstadisticaService(
            EstadisticaRepository estadisticaRepository) {
        this.estadisticaRepository = estadisticaRepository;
    }

    public List<EstadisticaEstadoDTO> obtenerTotalesPorEstado() {
        return estadisticaRepository.contarPorEstado();
    }
}