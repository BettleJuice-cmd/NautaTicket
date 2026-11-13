
package ni.edu.uam.nautaticket.controller;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
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

    private static final System.Logger LOG = System.getLogger(AtencionController.class.getName());

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
    @FXML private Button botonActualizar;
    @FXML private Button botonTomar;
    @FXML private Button botonIniciar;
    @FXML private Button botonResolver;
    @FXML private ProgressIndicator progresoToma;

    private AtencionService servicio;
    private final ObjectProperty<Task<Toma>> tomaActiva = new SimpleObjectProperty<>();

    private record Toma(int idTicket, List<TicketFila> pendientes, List<TicketFila> asignados) {}

    private final DateTimeFormatter formato =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        var ocupada = tomaEnCurso();
        botonActualizar.disableProperty().bind(ocupada);
        botonTomar.disableProperty().bind(ocupada);
        botonIniciar.disableProperty().bind(ocupada);
        botonResolver.disableProperty().bind(ocupada);
        campoSolucion.disableProperty().bind(ocupada);
        progresoToma.visibleProperty().bind(ocupada);
        progresoToma.managedProperty().bind(ocupada);

        columnaPendienteId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().idTicket()));

        columnaPendienteTitulo.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().titulo()));

        columnaPendienteFecha.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().fechaCreacion().format(formato)));

        columnaMioId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().idTicket()));

        columnaMioTitulo.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().titulo()));

        columnaMioEstado.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().estado()));
    }

    BooleanBinding tomaEnCurso() {
        return tomaActiva.isNotNull();
    }

    public void setServicio(AtencionService servicio) {
        this.servicio = servicio;
        actualizar();
    }

    public void setIdTecnico(int idTecnico) {
        campoTecnico.setText(String.valueOf(idTecnico));
        campoTecnico.setDisable(true);

        if (servicio != null) {
            actualizar();
        }
    }

    private int tecnico() {
        try {
            int id = Integer.parseInt(campoTecnico.getText().trim());

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Introduce el ID numerico del tecnico.");
        }
    }

    @FXML
    private void tomarSiguiente() {
        if (tomaActiva.get() != null) {
            return;
        }

        AtencionService servicioActual = servicio;
        if (servicioActual == null) {
            mensaje.setText("El servicio de atención no está conectado.");
            return;
        }

        int idTecnico;
        try {
            idTecnico = tecnico();
        } catch (IllegalArgumentException e) {
            mensaje.setText(e.getMessage());
            return;
        }

        Task<Toma> tarea = new Task<>() {
            @Override
            protected Toma call() {
                int id = servicioActual.tomarSiguienteTicket(idTecnico);
                if (isCancelled()) {
                    return null;
                }
                try {
                    return new Toma(id, servicioActual.listarPendientes(),
                            servicioActual.listarMisTickets(idTecnico));
                } catch (RuntimeException e) {
                    LOG.log(System.Logger.Level.WARNING, "No se pudieron recargar las listas tras la toma FIFO", e);
                    return new Toma(id, null, null);
                }
            }
        };
        tarea.setOnSucceeded(evento -> mostrarToma(tarea.getValue()));
        tarea.setOnFailed(evento -> mostrarErrorToma(tarea.getException()));
        tarea.setOnCancelled(evento -> mensaje.setText("Cancelando la espera de la toma FIFO…"));

        Thread hilo = new Thread(() -> {
            try {
                tarea.run();
            } finally {
                // La cancelación de Task puede adelantarse al final de la llamada JDBC.
                Platform.runLater(() -> {
                    if (tarea.isCancelled()) {
                        mensaje.setText("Toma cancelada. Actualiza las listas para comprobar la asignación.");
                    }
                    tomaActiva.set(null);
                });
            }
        }, "nautaticket-fifo");
        hilo.setDaemon(true);

        mensaje.setText("Asignando el siguiente ticket…");
        tomaActiva.set(tarea);
        try {
            hilo.start();
        } catch (RuntimeException e) {
            tomaActiva.set(null);
            mostrarErrorToma(e);
        }
    }

    private void mostrarToma(Toma toma) {
        String confirmacion = "Ticket #" + toma.idTicket() + " asignado correctamente.";
        if (toma.pendientes() == null) {
            mensaje.setText(confirmacion + " No se pudieron recargar las listas. Pulsa Actualizar listas.");
            return;
        }

        tablaPendientes.setItems(FXCollections.observableArrayList(toma.pendientes()));
        tablaMisTickets.setItems(FXCollections.observableArrayList(toma.asignados()));
        mensaje.setText(confirmacion);
    }

    private void mostrarErrorToma(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) {
            mensaje.setText(error.getMessage());
        } else {
            LOG.log(System.Logger.Level.ERROR, "Falló la toma FIFO", error);
            mensaje.setText("No se pudo asignar el ticket. Revisa la conexión e inténtalo de nuevo.");
        }
    }

    @FXML
    private void iniciar() {
        ejecutar(() -> {
            TicketFila fila = seleccionado();

            servicio.iniciarAtencion(fila.idTicket(), tecnico());

            mensaje.setText(
                    "Atencion iniciada para el ticket #" + fila.idTicket());
        });
    }

    @FXML
    private void resolver() {
        ejecutar(() -> {
            TicketFila fila = seleccionado();

            servicio.resolverTicket(
                    fila.idTicket(),
                    tecnico(),
                    campoSolucion.getText()
            );

            campoSolucion.clear();

            mensaje.setText(
                    "Ticket #" + fila.idTicket() + " resuelto correctamente.");
        });
    }

    private TicketFila seleccionado() {
        TicketFila fila =
                tablaMisTickets.getSelectionModel().getSelectedItem();

        if (fila == null) {
            throw new IllegalArgumentException(
                    "Selecciona un ticket de Mis tickets.");
        }

        return fila;
    }

    @FXML
    private void actualizar() {
        if (tomaActiva.get() != null) {
            return;
        }
        if (servicio == null) {
            mensaje.setText("El servicio de atencion no esta conectado.");
            return;
        }

        try {
            tablaPendientes.setItems(
                    FXCollections.observableArrayList(
                            servicio.listarPendientes()
                    )
            );

            if (campoTecnico.getText() != null &&
                    !campoTecnico.getText().isBlank()) {

                tablaMisTickets.setItems(
                        FXCollections.observableArrayList(
                                servicio.listarMisTickets(tecnico())
                        )
                );

            } else {
                tablaMisTickets.getItems().clear();
            }

        } catch (RuntimeException e) {
            mensaje.setText("Error al cargar: " + e.getMessage());
        }
    }

    private void ejecutar(Runnable accion) {
        if (tomaActiva.get() != null) {
            return;
        }
        if (servicio == null) {
            mensaje.setText("El servicio de atencion no esta conectado.");
            return;
        }

        try {
            accion.run();
            actualizar();

        } catch (RuntimeException e) {
            mensaje.setText("Error: " + e.getMessage());
        }
    }
}
