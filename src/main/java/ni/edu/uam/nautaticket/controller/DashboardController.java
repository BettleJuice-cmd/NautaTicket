package ni.edu.uam.nautaticket.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.concurrent.Task;

import java.io.File;
import java.time.LocalDate;
import ni.edu.uam.nautaticket.service.HistorialService;

import java.util.Map;

public class DashboardController {
    @FXML private DatePicker fechaDesde;
    @FXML private DatePicker fechaHasta;
    @FXML private ComboBox<String> filtroEstado;
    @FXML private Label pendientes;
    @FXML private Label enProceso;
    @FXML private Label resueltos;
    @FXML private Label cerrados;
    @FXML private Button botonReporte;
    @FXML private Label mensajeReporte;
    private HistorialService historialService;
    private ReportePdfAction reportePdfAction;
    private boolean generandoReporte;

    @FXML
    private void initialize() {
        filtroEstado.getItems().addAll("TODOS", "PENDIENTE", "EN_PROCESO", "RESUELTO", "CERRADO");
        filtroEstado.setValue("TODOS");
        botonReporte.setDisable(true);
        mensajeReporte.setText("Generación de PDF pendiente de integrar con ReporteService.");
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
            cerrados.setText(String.valueOf(totales.getOrDefault("CERRADO", 0L)));
        } catch (RuntimeException ex) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setHeaderText("No se pudieron cargar las estadísticas");
            alerta.setContentText(ex instanceof IllegalArgumentException
                    ? ex.getMessage() : "Revisa la conexión y vuelve a intentarlo.");
            alerta.showAndWait();
        }
    }

    /**
     * Contrato de interfaz temporal. Carlos adaptará aquí su ReporteService
     * cuando el equipo acuerde la firma real del método de exportación.
     */
    public void setReportePdfAction(ReportePdfAction action) {
        this.reportePdfAction = action;
        botonReporte.setDisable(action == null || generandoReporte);
        mensajeReporte.setText(action == null
                ? "Generación de PDF pendiente de integrar con ReporteService."
                : "Reporte disponible: selecciona los filtros y genera el PDF.");
    }

    @FXML
    private void generarReportePdf() {
        if (reportePdfAction == null || generandoReporte) {
            return;
        }
        final LocalDate desde = fechaDesde.getValue();
        final LocalDate hasta = fechaHasta.getValue();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            mostrarErrorReporte("La fecha inicial no puede ser posterior a la final.");
            return;
        }
        final String estado = "TODOS".equals(filtroEstado.getValue())
                ? null : filtroEstado.getValue();
        Window ventana = botonReporte.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar reporte de tickets");
        chooser.setInitialFileName("NautaTicket-reporte.pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        File destino = chooser.showSaveDialog(ventana);
        if (destino == null) return;

        // Capturar todo el estado de la UI ANTES de iniciar el hilo de fondo.
        final ReportePdfAction action = reportePdfAction;
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                action.generar(destino.toPath(), desde, hasta, estado);
                return null;
            }
        };
        generandoReporte = true;
        botonReporte.setDisable(true);
        mensajeReporte.setText("Generando reporte PDF...");
        tarea.setOnSucceeded(e -> {
            generandoReporte = false;
            botonReporte.setDisable(reportePdfAction == null);
            mensajeReporte.setText("Reporte guardado en: " + destino.getAbsolutePath());
            Alert aviso = new Alert(Alert.AlertType.INFORMATION);
            aviso.setHeaderText("Reporte generado correctamente");
            aviso.setContentText("Se guardó el archivo PDF en la ubicación seleccionada.");
            aviso.showAndWait();
        });
        tarea.setOnFailed(e -> {
            generandoReporte = false;
            botonReporte.setDisable(reportePdfAction == null);
            System.getLogger(DashboardController.class.getName())
                    .log(System.Logger.Level.ERROR, "Error al generar reporte PDF", tarea.getException());
            mostrarErrorReporte("No se pudo generar el PDF. Comprueba los filtros y la ubicación de destino.");
        });
        Thread hilo = new Thread(tarea, "nautaticket-exportar-pdf");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarErrorReporte(String texto) {
        mensajeReporte.setText(texto);
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setHeaderText("No se pudo generar el reporte");
        alerta.setContentText(texto);
        alerta.showAndWait();
    }

    @FXML
    private void limpiar() {
        fechaDesde.setValue(null);
        fechaHasta.setValue(null);
        filtroEstado.setValue("TODOS");
        actualizar();
    }
}
