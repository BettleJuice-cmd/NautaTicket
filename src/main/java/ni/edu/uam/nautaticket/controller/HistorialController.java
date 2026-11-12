package ni.edu.uam.nautaticket.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ni.edu.uam.nautaticket.model.HistorialTicket;
import ni.edu.uam.nautaticket.service.HistorialService;

import java.time.format.DateTimeFormatter;

public class HistorialController {

    @FXML private TextField ticket;
    @FXML private DatePicker desde;
    @FXML private DatePicker hasta;
    @FXML private ComboBox<String> estado;
    @FXML private Label mensaje;
    @FXML private TableView<HistorialTicket> movimientos;
    @FXML private TableColumn<HistorialTicket, String> colFecha;
    @FXML private TableColumn<HistorialTicket, String> colUsuario;
    @FXML private TableColumn<HistorialTicket, String> colAnterior;
    @FXML private TableColumn<HistorialTicket, String> colNuevo;
    @FXML private TableColumn<HistorialTicket, String> colComentario;

    private final DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private HistorialService historialService;

    @FXML
    private void initialize() {
        estado.getItems().addAll("TODOS", "PENDIENTE", "EN_PROCESO", "RESUELTO", "CERRADO");
        estado.setValue("TODOS");
        colFecha.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().getFechaCambio().format(formato)));
        colUsuario.setCellValueFactory(fila -> {
            var usuario = fila.getValue().getUsuario();
            return new ReadOnlyStringWrapper(usuario.getNombre() + " " + usuario.getApellido());
        });
        colAnterior.setCellValueFactory(fila -> new ReadOnlyStringWrapper(fila.getValue().getEstadoAnterior()));
        colNuevo.setCellValueFactory(fila -> new ReadOnlyStringWrapper(fila.getValue().getEstadoNuevo()));
        colComentario.setCellValueFactory(fila -> new ReadOnlyStringWrapper(fila.getValue().getComentario()));
    }

    public void setHistorialService(HistorialService historialService) {
        this.historialService = historialService;
    }

    @FXML
    private void buscar() {
        movimientos.getItems().clear();
        try {
            int id = Integer.parseInt(ticket.getText().trim());
            String filtro = estado.getValue();
            var resultados = historialService.historialPorTicket(
                    id, desde.getValue(), hasta.getValue(), "TODOS".equals(filtro) ? null : filtro);
            movimientos.getItems().setAll(resultados);
            mensaje.setText(resultados.isEmpty() ? "No hay movimientos para el ticket y los filtros indicados." : "");
        } catch (NumberFormatException e) {
            mensaje.setText("Introduce un ID de ticket válido.");
        } catch (IllegalArgumentException e) {
            mensaje.setText(e.getMessage());
        } catch (RuntimeException e) {
            mensaje.setText("No se pudo consultar el historial. Revisa la conexión e inténtalo de nuevo.");
            System.getLogger(HistorialController.class.getName())
                    .log(System.Logger.Level.ERROR, "Error al consultar el historial", e);
        }
    }

    @FXML
    private void limpiar() {
        ticket.clear();
        desde.setValue(null);
        hasta.setValue(null);
        estado.setValue("TODOS");
        movimientos.getItems().clear();
        mensaje.setText("");
    }
}
