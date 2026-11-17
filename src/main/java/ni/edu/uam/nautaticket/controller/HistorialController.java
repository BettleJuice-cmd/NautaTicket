package ni.edu.uam.nautaticket.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.converter.LocalDateStringConverter;

import ni.edu.uam.nautaticket.model.HistorialTicket;
import ni.edu.uam.nautaticket.service.HistorialService;

import java.time.LocalDate;
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

    private final DateTimeFormatter formato =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private HistorialService historialService;

    @FXML
    private void initialize() {
        estado.getItems().addAll(
                "TODOS",
                "PENDIENTE",
                "EN_PROCESO",
                "RESUELTO",
                "CERRADO"
        );
        estado.setValue("TODOS");
        colFecha.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(
                        fila.getValue()
                                .getFechaCambio()
                                .format(formato)
                )
        );

        colUsuario.setCellValueFactory(fila -> {

            var usuario = fila.getValue().getUsuario();

            return new ReadOnlyStringWrapper(
                    usuario.getNombre() + " " +
                            usuario.getApellido()
            );
        });

        colAnterior.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(
                        fila.getValue().getEstadoAnterior()
                )
        );

        colNuevo.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(
                        fila.getValue().getEstadoNuevo()
                )
        );

        colComentario.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(
                        fila.getValue().getComentario()
                )
        );
    }

    public void setHistorialService(
            HistorialService historialService
    ) {

        if (historialService == null) {
            throw new IllegalArgumentException(
                    "El servicio de historial es obligatorio."
            );
        }

        this.historialService = historialService;

        // Cargar automáticamente el historial
        // al abrir la pantalla por primera vez.
        buscar();
    }

    @FXML
    private void buscar() {

        if (historialService == null) {
            mensaje.setText(
                    "El servicio de historial no está disponible."
            );
            return;
        }

        try {

            String textoId = ticket.getText().trim();

            Integer id = textoId.isEmpty()
                    ? null
                    : Integer.parseInt(textoId);

            String filtro = estado.getValue();

            var resultados = historialService.buscar(
                    id,
                    leerFecha(desde, "desde"),
                    leerFecha(hasta, "hasta"),
                    "TODOS".equals(filtro) ? null : filtro
            );

            movimientos.getItems().setAll(resultados);

            mensaje.setText(
                    resultados.isEmpty()
                            ? "No hay movimientos para los filtros indicados."
                            : ""
            );

        } catch (NumberFormatException e) {

            mensaje.setText(
                    "El ID del ticket debe ser un número entero positivo."
            );

        } catch (IllegalArgumentException e) {

            mensaje.setText(e.getMessage());

        } catch (RuntimeException e) {

            mensaje.setText(
                    "No se pudo consultar el historial. " +
                            "Revisa la conexión e inténtalo de nuevo."
            );

            System.getLogger(
                    HistorialController.class.getName()
            ).log(
                    System.Logger.Level.ERROR,
                    "Error al consultar el historial",
                    e
            );
        }
    }

    private LocalDate leerFecha(
            DatePicker selector,
            String nombre
    ) {

        String texto = selector.getEditor()
                .getText()
                .trim();

        if (texto.isEmpty()) {

            selector.setValue(null);
            return null;
        }

        try {

            var conversor = selector.getConverter();

            LocalDate fecha = (
                    conversor == null
                            ? new LocalDateStringConverter()
                            : conversor
            ).fromString(texto);

            if (fecha == null) {
                throw new IllegalArgumentException();
            }

            selector.setValue(fecha);

            return fecha;

        } catch (RuntimeException e) {

            throw new IllegalArgumentException(
                    "La fecha " + nombre + " no es válida.",
                    e
            );
        }
    }

    @FXML
    private void limpiar() {

        ticket.clear();

        desde.setValue(null);
        desde.getEditor().clear();

        hasta.setValue(null);
        hasta.getEditor().clear();

        estado.setValue("TODOS");

        mensaje.setText("");

        // Restaurar automáticamente todos los
        // movimientos después de limpiar filtros.
        buscar();
    }
}
