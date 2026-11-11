
package ni.edu.uam.nautaticket.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.nautaticket.dao.AsignacionTicketDao.TicketFila;
import ni.edu.uam.nautaticket.service.AtencionService;

import java.time.format.DateTimeFormatter;

public class AtencionController {

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

    private AtencionService servicio;

    private final DateTimeFormatter formato =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {

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
        ejecutar(() -> {
            int id = servicio.tomarSiguienteTicket(tecnico());
            mensaje.setText("Ticket #" + id + " asignado correctamente.");
        });
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
