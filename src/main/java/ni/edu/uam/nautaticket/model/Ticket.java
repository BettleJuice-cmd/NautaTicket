package ni.edu.uam.nautaticket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ticket")
    private Integer idTicket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitante", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<AsignacionTicket> asignaciones = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "id_tecnico")
    private Usuario tecnico;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado", nullable = false)
    private EstadoTicket estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "solucion", columnDefinition = "TEXT")
    private String solucion;

    public List<AsignacionTicket> getAsignaciones() {
        return Collections.unmodifiableList(asignaciones);
    }

    public void agregarAsignacion(AsignacionTicket asignacion) {
        Objects.requireNonNull(asignacion, "La asignación es obligatoria.");
        if (asignacion.getTicket() != null && asignacion.getTicket() != this) {
            throw new IllegalArgumentException("La asignación pertenece a otro ticket.");
        }
        if (asignacion.getIdAsignacion() != null) {
            throw new IllegalStateException("Una asignación guardada no puede agregarse de nuevo.");
        }
        if (!asignaciones.contains(asignacion)) {
            asignaciones.add(asignacion);
        }
        asignacion.setTicket(this);
    }

    public void retirarAsignacion(AsignacionTicket asignacion) {
        if (asignacion == null || asignacion.getTicket() != this) {
            return;
        }
        if (asignacion.getIdAsignacion() != null) {
            throw new IllegalStateException("Una asignación guardada forma parte del historial.");
        }
        asignaciones.remove(asignacion);
        asignacion.setTicket(null);
    }

    @PrePersist
    public void antesDeGuardar() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
