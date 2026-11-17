
package ni.edu.uam.nautaticket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteTicketDTO {

    private Integer idTicket;
    private String departamento;
    private String titulo;
    private String categoria;
    private String tecnicoAsignado;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
}
