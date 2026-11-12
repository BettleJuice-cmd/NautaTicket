
package ni.edu.uam.nautaticket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "asignaciones_ticket")
@Getter
@Setter
@NoArgsConstructor
public class AsignacionTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Integer idAsignacion;

    @Column(name = "id_ticket", nullable = false)
    private Integer idTicket;

    @Column(name = "id_tecnico", nullable = false)
    private Integer idTecnico;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    public AsignacionTicket(Integer idTicket, Integer idTecnico) {
        this.idTicket = idTicket;
        this.idTecnico = idTecnico;
        this.fechaAsignacion = LocalDateTime.now();
    }
}
