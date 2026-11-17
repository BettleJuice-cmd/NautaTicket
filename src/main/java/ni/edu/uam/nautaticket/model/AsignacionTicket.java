
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ticket", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tecnico", nullable = false)
    private Usuario tecnico;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    public AsignacionTicket(Ticket ticket, Usuario tecnico) {
        this.ticket = ticket;
        this.tecnico = tecnico;
        this.fechaAsignacion = LocalDateTime.now();
    }
}
