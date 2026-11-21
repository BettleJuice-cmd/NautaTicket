package ni.edu.uam.nautaticket.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import net.sf.jasperreports.engine.JasperPrint;

import ni.edu.uam.nautaticket.dto.EstadisticaApiDTO;
import ni.edu.uam.nautaticket.service.EstadisticasApiClient;
import ni.edu.uam.nautaticket.service.HistorialService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @FXML private Button botonApi;

    private HistorialService historialService;
    private ReportePdfAction reportePdfAction;

    private final EstadisticasApiClient apiClient =
            new EstadisticasApiClient();

    private boolean generandoReporte;
    private boolean consultandoApi;

    @FXML
    private void initialize() {
        filtroEstado.getItems().addAll(
                "TODOS",
                "PENDIENTE",
                "EN_PROCESO",
                "RESUELTO",
                "CERRADO"
        );

        filtroEstado.setValue("TODOS");

        botonReporte.setDisable(true);
        mensajeReporte.setText(
                "Generación de PDF pendiente de integrar con ReporteService."
        );

        botonApi.setVisible(false);
        botonApi.setManaged(false);
        botonApi.setDisable(true);
    }

    public void setHistorialService(
            HistorialService historialService
    ) {
        this.historialService = historialService;
        actualizar();
    }

    @FXML
    private void actualizar() {
        if (historialService == null) return;

        try {
            String estado = filtroEstado.getValue();

            Map<String, Long> totales = historialService.totales(
                    fechaDesde.getValue(),
                    fechaHasta.getValue(),
                    "TODOS".equals(estado) ? null : estado
            );

            pendientes.setText(String.valueOf(
                    totales.getOrDefault("PENDIENTE", 0L)
            ));

            enProceso.setText(String.valueOf(
                    totales.getOrDefault("EN_PROCESO", 0L)
            ));

            resueltos.setText(String.valueOf(
                    totales.getOrDefault("RESUELTO", 0L)
            ));

            cerrados.setText(String.valueOf(
                    totales.getOrDefault("CERRADO", 0L)
            ));

        } catch (RuntimeException ex) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setHeaderText(
                    "No se pudieron cargar las estadísticas"
            );
            alerta.setContentText(
                    ex instanceof IllegalArgumentException
                            ? ex.getMessage()
                            : "Revisa la conexión y vuelve a intentarlo."
            );
            alerta.showAndWait();
        }
    }

    // Carlos conectará aquí el servicio real de JasperReports.
    public void setReportePdfAction(ReportePdfAction action) {
        this.reportePdfAction = action;

        botonReporte.setDisable(
                action == null || generandoReporte
        );

        mensajeReporte.setText(
                action == null
                        ? "Generación de PDF pendiente de integrar con ReporteService."
                        : "Reporte disponible: selecciona los filtros y genera el PDF."
        );
    }

    @FXML
    private void generarReportePdf() {
        if (reportePdfAction == null || generandoReporte) {
            return;
        }

        final LocalDate desde = fechaDesde.getValue();
        final LocalDate hasta = fechaHasta.getValue();

        if (desde != null && hasta != null
                && desde.isAfter(hasta)) {
            mostrarErrorReporte(
                    "La fecha inicial no puede ser posterior a la final."
            );
            return;
        }

        final String estado =
                "TODOS".equals(filtroEstado.getValue())
                        ? null
                        : filtroEstado.getValue();

        Window ventana = botonReporte.getScene().getWindow();

        final ReportePdfAction action = reportePdfAction;

        Task<JasperPrint> tarea = new Task<>() {
            @Override
            protected JasperPrint call() throws Exception {
                return action.preparar(desde, hasta, estado);
            }
        };

        generandoReporte = true;
        botonReporte.setDisable(true);
        mensajeReporte.setText("Preparando vista previa...");

        tarea.setOnSucceeded(e -> {
            try {
                VistaPreviaReporte.mostrar(ventana, tarea.getValue(),
                        () -> true,
                        archivo -> {
                            generandoReporte = false;
                            botonReporte.setDisable(reportePdfAction == null);
                            mensajeReporte.setText("Reporte guardado en: " + archivo);
                        },
                        () -> {
                            generandoReporte = false;
                            botonReporte.setDisable(reportePdfAction == null);
                            mensajeReporte.setText("Vista previa cerrada.");
                        });
            } catch (RuntimeException error) {
                generandoReporte = false;
                botonReporte.setDisable(reportePdfAction == null);
                mostrarErrorReporte("No se pudo mostrar la vista previa.");
            }
        });

        tarea.setOnFailed(e -> {
            generandoReporte = false;
            botonReporte.setDisable(reportePdfAction == null);

            System.getLogger(
                    DashboardController.class.getName()
            ).log(
                    System.Logger.Level.ERROR,
                    "Error al preparar reporte",
                    tarea.getException()
            );

            mostrarErrorReporte(
                    "No se pudo preparar el reporte. Comprueba los filtros y la conexión."
            );
        });

        Thread hilo = new Thread(
                tarea,
                "nautaticket-exportar-pdf"
        );

        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarErrorReporte(String texto) {
        mensajeReporte.setText(texto);

        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setHeaderText(
                "No se pudo generar el reporte"
        );
        alerta.setContentText(texto);
        alerta.showAndWait();
    }

    // Se habilitará desde PanelController solo para ADMIN.
    public void setApiDisponible(boolean esAdmin) {
        botonApi.setVisible(esAdmin);
        botonApi.setManaged(esAdmin);
        botonApi.setDisable(!esAdmin || consultandoApi);
    }

    @FXML
    private void consultarApiRest() {
        if (botonApi.isDisabled() || consultandoApi) {
            return;
        }

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("NautaTicket - API REST");
        dialogo.setHeaderText(
                "Autenticación de administrador"
        );

        ButtonType consultar = new ButtonType(
                "Consultar",
                ButtonBar.ButtonData.OK_DONE
        );

        dialogo.getDialogPane().getButtonTypes().addAll(
                consultar,
                ButtonType.CANCEL
        );

        TextField correo = new TextField();
        correo.setPromptText("Correo del administrador");

        PasswordField password = new PasswordField();
        password.setPromptText("Contraseña");

        VBox contenido = new VBox(10);
        contenido.getChildren().addAll(
                new Label("Correo electrónico"),
                correo,
                new Label("Contraseña"),
                password
        );

        dialogo.getDialogPane().setContent(contenido);

        Optional<ButtonType> respuesta =
                dialogo.showAndWait();

        if (respuesta.isEmpty()
                || respuesta.get() != consultar) {
            return;
        }

        String correoIngresado = correo.getText().trim();
        String passwordIngresado = password.getText();

        password.clear();

        if (correoIngresado.isBlank()
                || passwordIngresado.isBlank()) {
            mostrarErrorApi(
                    "Debes ingresar el correo y la contraseña."
            );
            return;
        }

        Task<List<EstadisticaApiDTO>> tarea = new Task<>() {
            @Override
            protected List<EstadisticaApiDTO> call()
                    throws Exception {
                return apiClient.consultar(
                        correoIngresado,
                        passwordIngresado
                );
            }
        };

        consultandoApi = true;
        botonApi.setDisable(true);
        botonApi.setText("Consultando API...");

        tarea.setOnSucceeded(e -> {
            finalizarConsultaApi();
            mostrarEstadisticasApi(tarea.getValue());
        });

        tarea.setOnFailed(e -> {
            finalizarConsultaApi();

            Throwable error = tarea.getException();

            System.getLogger(
                    DashboardController.class.getName()
            ).log(
                    System.Logger.Level.ERROR,
                    "Error en la consulta REST",
                    error
            );

            String mensaje =
                    "No se pudo consultar la API REST.";

            if (error instanceof java.net.ConnectException) {
                mensaje = "No se pudo conectar con Spring Boot. "
                        + "Comprueba que la API esté ejecutándose "
                        + "en el puerto 8081.";
            } else if (error instanceof java.net.http.HttpTimeoutException) {
                mensaje = "La API tardó demasiado en responder.";
            } else if (error instanceof java.io.IOException
                    && error.getMessage() != null) {
                mensaje = error.getMessage();
            }

            mostrarErrorApi(mensaje);
        });

        Thread hilo = new Thread(
                tarea,
                "nautaticket-consulta-api"
        );

        hilo.setDaemon(true);
        hilo.start();
    }

    private void finalizarConsultaApi() {
        consultandoApi = false;
        botonApi.setDisable(false);
        botonApi.setText("Consultar API REST");
    }

    private void mostrarEstadisticasApi(
            List<EstadisticaApiDTO> estadisticas
    ) {
        StringBuilder resultado = new StringBuilder();

        for (EstadisticaApiDTO estadistica : estadisticas) {
            resultado.append(estadistica.estado())
                    .append(": ")
                    .append(estadistica.total())
                    .append("\n");
        }

        Alert aviso = new Alert(
                Alert.AlertType.INFORMATION
        );

        aviso.setTitle("NautaTicket - API REST");
        aviso.setHeaderText(
                "Estadísticas obtenidas desde Spring Boot"
        );

        aviso.setContentText(
                resultado.length() == 0
                        ? "No se encontraron estadísticas."
                        : resultado.toString()
        );

        aviso.showAndWait();
    }

    private void mostrarErrorApi(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("NautaTicket - API REST");
        alerta.setHeaderText(
                "Error al consultar la API"
        );
        alerta.setContentText(mensaje);
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
