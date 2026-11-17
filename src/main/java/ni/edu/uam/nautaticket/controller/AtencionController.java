package ni.edu.uam.nautaticket.controller;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import ni.edu.uam.nautaticket.dao.AsignacionTicketDao.TicketFila;
import ni.edu.uam.nautaticket.service.AtencionService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AtencionController {

    private static final System.Logger LOG =
            System.getLogger(AtencionController.class.getName());

    @FXML private TableView<TicketFila> tablaPendientes;
    @FXML private TableView<TicketFila> tablaMisTickets;

    @FXML private TableColumn<TicketFila, Number> columnaPendienteId;
    @FXML private TableColumn<TicketFila, String> columnaPendienteTitulo;
    @FXML private TableColumn<TicketFila, String> columnaPendienteFecha;

    @FXML private TableColumn<TicketFila, Number> columnaMioId;
    @FXML private TableColumn<TicketFila, String> columnaMioTitulo;
    @FXML private TableColumn<TicketFila, String> columnaMioEstado;

    @FXML private TextField campoTecnico;
    @FXML private TextArea campoSolucion;
    @FXML private Label mensaje;
    @FXML private Label etiquetaCapacidad;

    @FXML private Button botonActualizar;
    @FXML private Button botonTomar;
    @FXML private Button botonIniciar;
    @FXML private Button botonResolver;

    @FXML private ProgressIndicator progresoToma;

    private AtencionService servicio;

    private final ObjectProperty<Task<?>> tareaActiva =
            new SimpleObjectProperty<>();

    private final BooleanProperty capacidadLlena =
            new SimpleBooleanProperty(false);

    private final DateTimeFormatter formato =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private record Resultado(
            List<TicketFila> pendientes,
            List<TicketFila> asignados,
            int activos,
            String aviso
    ) {}

    @FunctionalInterface
    private interface Operacion {
        String ejecutar(AtencionService servicio, int idTecnico);
    }

    @FXML
    private void initialize() {
        BooleanBinding ocupada = tomaEnCurso();

        botonActualizar.disableProperty().bind(ocupada);
        botonTomar.disableProperty().bind(
                ocupada.or(capacidadLlena)
        );

        botonIniciar.disableProperty().bind(ocupada);
        botonResolver.disableProperty().bind(ocupada);
        campoSolucion.disableProperty().bind(ocupada);

        progresoToma.visibleProperty().bind(ocupada);
        progresoToma.managedProperty().bind(ocupada);

        columnaPendienteId.setCellValueFactory(
                fila -> new ReadOnlyObjectWrapper<>(
                        fila.getValue().idTicket()
                )
        );

        columnaPendienteTitulo.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(
                        fila.getValue().titulo()
                )
        );

        columnaPendienteFecha.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(
                        fila.getValue().fechaCreacion().format(formato)
                )
        );

        columnaMioId.setCellValueFactory(
                fila -> new ReadOnlyObjectWrapper<>(
                        fila.getValue().idTicket()
                )
        );

        columnaMioTitulo.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(
                        fila.getValue().titulo()
                )
        );

        columnaMioEstado.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(
                        fila.getValue().estado()
                )
        );

        etiquetaCapacidad.setText(
                "Mis tickets activos: -- / "
                        + AtencionService.MAX_TICKETS_ACTIVOS
        );
    }

    public BooleanBinding tomaEnCurso() {
        return tareaActiva.isNotNull();
    }

    public void setIdTecnico(int idTecnico) {
        campoTecnico.setText(String.valueOf(idTecnico));
        campoTecnico.setDisable(true);

        if (servicio != null) {
            actualizar();
        }
    }

    public void setServicio(AtencionService servicio) {
        this.servicio = servicio;

        if (campoTecnico.getText() != null
                && !campoTecnico.getText().isBlank()) {
            actualizar();
        }
    }

    private int tecnico() {
        try {
            int id = Integer.parseInt(
                    campoTecnico.getText().trim()
            );

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El ID del técnico no es válido."
            );
        }
    }

    private TicketFila seleccionado() {
        TicketFila fila = tablaMisTickets
                .getSelectionModel()
                .getSelectedItem();

        if (fila == null) {
            throw new IllegalArgumentException(
                    "Selecciona un ticket de Mis tickets."
            );
        }

        return fila;
    }

    @FXML
    private void actualizar() {
        ejecutarOperacion((actual, tecnicoId) ->
                "Listas actualizadas."
        );
    }

    @FXML
    private void tomarSiguiente() {
        ejecutarOperacion((actual, tecnicoId) -> {
            int idTicket = actual.tomarSiguienteTicket(tecnicoId);

            return "Ticket #" + idTicket
                    + " asignado correctamente.";
        });
    }

    @FXML
    private void iniciar() {
        final int idTicket;

        try {
            idTicket = seleccionado().idTicket();
        } catch (IllegalArgumentException e) {
            mensaje.setText(e.getMessage());
            return;
        }

        ejecutarOperacion((actual, tecnicoId) -> {
            actual.iniciarAtencion(idTicket, tecnicoId);

            return "Atención iniciada para el ticket #"
                    + idTicket + ".";
        });
    }

    @FXML
    private void resolver() {
        final int idTicket;

        try {
            idTicket = seleccionado().idTicket();
        } catch (IllegalArgumentException e) {
            mensaje.setText(e.getMessage());
            return;
        }

        final String solucion = campoSolucion.getText();

        if (solucion == null || solucion.isBlank()) {
            mensaje.setText(
                    "Debes escribir la solución del ticket."
            );
            return;
        }

        ejecutarOperacion((actual, tecnicoId) -> {
            actual.resolverTicket(
                    idTicket,
                    tecnicoId,
                    solucion
            );

            return "Ticket #" + idTicket
                    + " resuelto correctamente.";
        }, true);
    }

    private void ejecutarOperacion(Operacion operacion) {
        ejecutarOperacion(operacion, false);
    }

    private void ejecutarOperacion(
            Operacion operacion,
            boolean limpiarSolucion
    ) {
        if (tareaActiva.get() != null) {
            return;
        }

        final AtencionService actual = servicio;

        if (actual == null) {
            mensaje.setText(
                    "El servicio de atención no está conectado."
            );
            return;
        }

        final int idTecnico;

        try {
            idTecnico = tecnico();
        } catch (IllegalArgumentException e) {
            mensaje.setText(e.getMessage());
            return;
        }

        Task<Resultado> tarea = new Task<>() {
            @Override
            protected Resultado call() {
                String aviso = operacion.ejecutar(
                        actual,
                        idTecnico
                );

                List<TicketFila> pendientes =
                        actual.listarPendientes();

                List<TicketFila> asignados =
                        actual.listarMisTickets(idTecnico);

                int activos = actual.contarTicketsActivos(
                        idTecnico
                );

                return new Resultado(
                        pendientes,
                        asignados,
                        activos,
                        aviso
                );
            }
        };

        tarea.setOnSucceeded(evento -> {
            Resultado resultado = tarea.getValue();

            tablaPendientes.setItems(
                    FXCollections.observableArrayList(
                            resultado.pendientes()
                    )
            );

            tablaMisTickets.setItems(
                    FXCollections.observableArrayList(
                            resultado.asignados()
                    )
            );

            actualizarCapacidad(resultado.activos());

            if (limpiarSolucion) {
                campoSolucion.clear();
            }

            mensaje.setText(resultado.aviso());
        });

        tarea.setOnFailed(evento -> {
            Throwable error = tarea.getException();
            LOG.log(System.Logger.Level.ERROR, "Error en Atención FIFO", error);

            if (error instanceof IllegalArgumentException
                    || error instanceof IllegalStateException) {
                mensaje.setText(error.getMessage());
            } else {
                mensaje.setText(
                        "No se pudo completar la operación. "
                                + "Revisa la conexión."
                );
            }
        });

        tarea.setOnCancelled(evento -> {
            mensaje.setText(
                    "Cancelando operación..."
            );
        });

        tareaActiva.set(tarea);
        mensaje.setText("Procesando operación...");

        Thread hilo = new Thread(() -> {
            try {
                tarea.run();
            } finally {
                Platform.runLater(() -> {
                    if (tarea.isCancelled()) {
                        mensaje.setText("Operación cancelada. Actualiza las listas para comprobar el resultado.");
                    }
                    tareaActiva.set(null);
                });
            }
        }, "nautaticket-atencion");

        hilo.setDaemon(true);
        try {
            hilo.start();
        } catch (RuntimeException error) {
            tareaActiva.set(null);
            LOG.log(System.Logger.Level.ERROR, "No se pudo iniciar la operación de atención", error);
            mensaje.setText("No se pudo iniciar la operación. Inténtalo de nuevo.");
        }
    }

    private void actualizarCapacidad(int activos) {
        int maximo = AtencionService.MAX_TICKETS_ACTIVOS;

        etiquetaCapacidad.setText(
                "Mis tickets activos: "
                        + activos + " / " + maximo
        );

        boolean llena = activos >= maximo;
        capacidadLlena.set(llena);

        etiquetaCapacidad.setStyle(
                llena
                        ? "-fx-text-fill: #b91c1c; "
                        + "-fx-font-weight: bold;"
                        : "-fx-text-fill: #285578; "
                        + "-fx-font-weight: bold;"
        );
    }
}
