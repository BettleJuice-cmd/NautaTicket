
package ni.edu.uam.nautaticket.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import ni.edu.uam.nautaticket.dao.AsignacionTicketDao.TicketFila;
import ni.edu.uam.nautaticket.service.AtencionService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AtencionController {

    private static final System.Logger LOG =
            System.getLogger(AtencionController.class.getName());

    private static final int INTERVALO_ACTUALIZACION = 5;

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

    private Timeline actualizacionAutomatica;

    private record Resultado(
            List<TicketFila> pendientes,
            List<TicketFila> asignados,
            int activos,
            String aviso
    ) {}

    @FunctionalInterface
    private interface Operacion {
        String ejecutar(
                AtencionService servicio,
                int idTecnico
        );
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
                        fila.getValue()
                                .fechaCreacion()
                                .format(formato)
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

        configurarActualizacionAutomatica();
    }

    public BooleanBinding tomaEnCurso() {
        return tareaActiva.isNotNull();
    }

    public void setIdTecnico(int idTecnico) {

        campoTecnico.setText(
                String.valueOf(idTecnico)
        );

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

        ejecutarOperacion(
                (actual, tecnicoId) ->
                        "Listas actualizadas.",
                false,
                false
        );
    }

    @FXML
    private void tomarSiguiente() {

        ejecutarOperacion((actual, tecnicoId) -> {

            int idTicket = actual.tomarSiguienteTicket(
                    tecnicoId
            );

            return "Ticket #" + idTicket
                    + " asignado correctamente.";

        }, false, false);
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

            actual.iniciarAtencion(
                    idTicket,
                    tecnicoId
            );

            return "Atención iniciada para el ticket #"
                    + idTicket + ".";

        }, false, false);
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

        }, true, false);
    }

    // Actualización automática cada cinco segundos.
    private void configurarActualizacionAutomatica() {

        actualizacionAutomatica = new Timeline(
                new KeyFrame(
                        Duration.seconds(INTERVALO_ACTUALIZACION),
                        evento -> actualizarEnSegundoPlano()
                )
        );

        actualizacionAutomatica.setCycleCount(
                Animation.INDEFINITE
        );

        // Iniciar cuando la pantalla se incorpora a JavaFX
        // y detener cuando se retira.
        tablaPendientes.sceneProperty().addListener(
                (observable, anterior, nueva) -> {

                    if (nueva == null) {

                        actualizacionAutomatica.stop();

                    } else {

                        actualizacionAutomatica.play();
                    }
                }
        );
    }

    private void actualizarEnSegundoPlano() {

        // No iniciar otra operación si hay una ejecutándose.
        if (tareaActiva.get() != null) {
            return;
        }

        if (servicio == null) {
            return;
        }

        if (tablaPendientes.getScene() == null) {
            return;
        }

        ejecutarOperacion(
                (actual, tecnicoId) -> null,
                false,
                true
        );
    }

    private void ejecutarOperacion(
            Operacion operacion,
            boolean limpiarSolucion,
            boolean silenciosa
    ) {

        if (tareaActiva.get() != null) {
            return;
        }

        final AtencionService actual = servicio;

        if (actual == null) {

            if (!silenciosa) {
                mensaje.setText(
                        "El servicio de atención no está conectado."
                );
            }

            return;
        }

        final int idTecnico;

        try {
            idTecnico = tecnico();

        } catch (IllegalArgumentException e) {

            if (!silenciosa) {
                mensaje.setText(e.getMessage());
            }

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

                int activos =
                        actual.contarTicketsActivos(idTecnico);

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

            // Recordar el ticket seleccionado.
            Integer pendienteSeleccionado =
                    obtenerIdSeleccionado(tablaPendientes);

            Integer mioSeleccionado =
                    obtenerIdSeleccionado(tablaMisTickets);

            // Actualizar sin sustituir el TableView.
            tablaPendientes.getItems().setAll(
                    resultado.pendientes()
            );

            tablaMisTickets.getItems().setAll(
                    resultado.asignados()
            );

            // Recuperar selecciones si siguen existiendo.
            restaurarSeleccion(
                    tablaPendientes,
                    pendienteSeleccionado
            );

            restaurarSeleccion(
                    tablaMisTickets,
                    mioSeleccionado
            );

            actualizarCapacidad(resultado.activos());

            if (limpiarSolucion) {
                campoSolucion.clear();
            }

            // No sobrescribir mensajes durante
            // una actualización automática.
            if (!silenciosa && resultado.aviso() != null) {
                mensaje.setText(resultado.aviso());
            }
        });

        tarea.setOnFailed(evento -> {

            Throwable error = tarea.getException();

            LOG.log(
                    System.Logger.Level.ERROR,
                    "Error en Atención FIFO",
                    error
            );

            if (error instanceof IllegalArgumentException
                    || error instanceof IllegalStateException) {

                mensaje.setText(error.getMessage());

            } else {

                mensaje.setText(
                        silenciosa
                                ? "No se pudo actualizar la cola automáticamente."
                                : "No se pudo completar la operación. Revisa la conexión."
                );
            }
        });

        tarea.setOnCancelled(evento -> {

            if (!silenciosa) {
                mensaje.setText(
                        "Cancelando operación..."
                );
            }
        });

        tareaActiva.set(tarea);

        if (!silenciosa) {
            mensaje.setText("Procesando operación...");
        }

        Thread hilo = new Thread(() -> {

            try {
                tarea.run();

            } finally {

                Platform.runLater(() -> {

                    if (tarea.isCancelled() && !silenciosa) {

                        mensaje.setText(
                                "Operación cancelada. Actualiza las listas para comprobar el resultado."
                        );
                    }

                    tareaActiva.set(null);
                });
            }

        }, silenciosa
                ? "nautaticket-actualizacion-auto"
                : "nautaticket-atencion");

        hilo.setDaemon(true);

        try {
            hilo.start();

        } catch (RuntimeException error) {

            tareaActiva.set(null);

            LOG.log(
                    System.Logger.Level.ERROR,
                    "No se pudo iniciar la operación de atención",
                    error
            );

            if (!silenciosa) {
                mensaje.setText(
                        "No se pudo iniciar la operación. Inténtalo de nuevo."
                );
            }
        }
    }

    private Integer obtenerIdSeleccionado(
            TableView<TicketFila> tabla
    ) {

        TicketFila seleccionado = tabla
                .getSelectionModel()
                .getSelectedItem();

        return seleccionado == null
                ? null
                : seleccionado.idTicket();
    }

    private void restaurarSeleccion(
            TableView<TicketFila> tabla,
            Integer idTicket
    ) {

        if (idTicket == null) {
            return;
        }

        for (TicketFila fila : tabla.getItems()) {

            if (fila.idTicket() == idTicket) {

                tabla.getSelectionModel().select(fila);
                return;
            }
        }
    }

    private void actualizarCapacidad(int activos) {

        int maximo =
                AtencionService.MAX_TICKETS_ACTIVOS;

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
