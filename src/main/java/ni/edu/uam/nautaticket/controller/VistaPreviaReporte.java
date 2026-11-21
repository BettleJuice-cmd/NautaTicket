package ni.edu.uam.nautaticket.controller;

import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.nautaticket.service.ReporteService;

import java.io.File;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Muestra el informe preparado y exporta exactamente el mismo JasperPrint. */
public final class VistaPreviaReporte {

    private VistaPreviaReporte() {}

    public static void mostrar(Window propietaria, JasperPrint documento,
                               BooleanSupplier autorizado,
                               Consumer<Path> guardado, Runnable cerrado) {
        String html = ReporteService.crearVistaPrevia(documento);
        Stage ventana = new Stage();
        ventana.setTitle("Vista previa del reporte");
        ventana.initOwner(propietaria);
        ventana.initModality(Modality.WINDOW_MODAL);

        WebView visor = new WebView();
        visor.getEngine().loadContent(html, "text/html");

        Button guardar = new Button("Guardar PDF");
        Button cancelar = new Button("Cerrar");
        HBox acciones = new HBox(10, guardar, cancelar);
        acciones.setStyle("-fx-padding: 10; -fx-alignment: center-right;");

        BorderPane contenido = new BorderPane(visor);
        contenido.setBottom(acciones);
        ventana.setScene(new Scene(contenido, 900, 700));

        guardar.setOnAction(evento -> {
            if (!autorizado.getAsBoolean()) {
                ventana.close();
                return;
            }
            FileChooser selector = new FileChooser();
            selector.setTitle("Guardar reporte de tickets");
            selector.setInitialFileName("NautaTicket_Reporte_Tickets.pdf");
            selector.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
            File destino = selector.showSaveDialog(ventana);
            if (destino == null) return;

            guardar.setDisable(true);
            cancelar.setDisable(true);
            Task<Path> tarea = new Task<>() {
                @Override protected Path call() {
                    if (!autorizado.getAsBoolean()) {
                        throw new SecurityException("La sesión ya no está disponible.");
                    }
                    return ReporteService.guardarReporte(documento, destino.toPath());
                }
            };
            tarea.setOnSucceeded(e -> {
                ventana.setOnHidden(null);
                ventana.close();
                guardado.accept(tarea.getValue());
            });
            tarea.setOnFailed(e -> {
                guardar.setDisable(false);
                cancelar.setDisable(false);
                Alert alerta = new Alert(Alert.AlertType.ERROR);
                alerta.initOwner(ventana);
                alerta.setHeaderText("No se pudo guardar el PDF");
                alerta.setContentText(tarea.getException().getMessage());
                alerta.showAndWait();
            });
            Thread hilo = new Thread(tarea, "nautaticket-exportar-pdf");
            hilo.setDaemon(true);
            hilo.start();
        });
        cancelar.setOnAction(evento -> ventana.close());
        ventana.setOnHidden(evento -> cerrado.run());
        ventana.show();
    }
}
