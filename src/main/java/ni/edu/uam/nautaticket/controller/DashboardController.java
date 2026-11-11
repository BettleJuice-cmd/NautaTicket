package ni.edu.uam.nautaticket.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import ni.edu.uam.nautaticket.service.HistorialService;

import java.util.Map;

public class DashboardController {
    @FXML private DatePicker fechaDesde;
    @FXML private DatePicker fechaHasta;
    @FXML private ComboBox<String> filtroEstado;
    @FXML private Label pendientes;
    @FXML private Label enProceso;
    @FXML private Label resueltos;
    private HistorialService historialService;

    @FXML
    private void initialize() {
        filtroEstado.getItems().addAll("TODOS", "PENDIENTE", "EN_PROCESO", "RESUELTO", "CERRADO");
        filtroEstado.setValue("TODOS");
    }

    // Invocar después de FXMLLoader.load() con la SessionFactory común del equipo.
    public void setHistorialService(HistorialService historialService) {
        this.historialService = historialService;
        actualizar();
    }

    @FXML
    private void actualizar() {
        if (historialService == null) return;
        try {
            String estado = filtroEstado.getValue();
            Map<String, Long> totales = historialService.totales(fechaDesde.getValue(),
                    fechaHasta.getValue(), "TODOS".equals(estado) ? null : estado);
            pendientes.setText(String.valueOf(totales.getOrDefault("PENDIENTE", 0L)));
            enProceso.setText(String.valueOf(totales.getOrDefault("EN_PROCESO", 0L)));
            resueltos.setText(String.valueOf(totales.getOrDefault("RESUELTO", 0L)));
        } catch (RuntimeException ex) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setHeaderText("No se pudieron cargar las estadísticas");
            alerta.setContentText(ex instanceof IllegalArgumentException
                    ? ex.getMessage() : "Revisa la conexión y vuelve a intentarlo.");
            alerta.showAndWait();
        }
    }

    @FXML
    private void limpiar() {
        fechaDesde.setValue(null);
        fechaHasta.setValue(null);
        filtroEstado.setValue("TODOS");
        actualizar();
    }
}
