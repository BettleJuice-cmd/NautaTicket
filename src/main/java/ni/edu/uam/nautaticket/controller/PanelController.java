

package ni.edu.uam.nautaticket.controller;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import ni.edu.uam.nautaticket.NautaTicketApp;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.AtencionService;
import ni.edu.uam.nautaticket.service.HistorialService;
import ni.edu.uam.nautaticket.service.ReporteService;
import ni.edu.uam.nautaticket.service.SesionAutenticada;
import ni.edu.uam.nautaticket.util.HibernateUtil;


import java.io.IOException;
import java.util.Locale;


public class PanelController {


    @FXML private ScrollPane contenido;
    @FXML private Label sesion;
    @FXML private Label mensaje;
    @FXML private Button botonAdministracion;
    @FXML private Button botonTickets;
    @FXML private Button botonAtencion;
    @FXML private Button botonHistorial;
    @FXML private Button botonDashboard;


    private Usuario usuario;
    private SesionAutenticada sesionAutenticada;
    private boolean administrador;
    private boolean tecnico;
    private boolean supervision;


    @FXML
    private void initialize() {
        mensaje.visibleProperty().bind(mensaje.textProperty().isNotEmpty());
        mensaje.managedProperty().bind(mensaje.visibleProperty());
    }


    private enum Vista {
        ADMINISTRACION("admin-view.fxml"),
        TICKETS("tickets.fxml"),
        ATENCION("atencion.fxml"),
        HISTORIAL("historial.fxml"),
        DASHBOARD("dashboard.fxml");


        private final String archivo;


        Vista(String archivo) {
            this.archivo = archivo;
        }
    }


    public void iniciar(Usuario usuario, SesionAutenticada sesionAutenticada) {
        if (usuario == null || usuario.getId() == null
                || !usuario.isActivo() || usuario.getRol() == null) {
            throw new IllegalArgumentException("La sesión no es válida.");
        }


        String rol = usuario.getRol().getNombre().toUpperCase(Locale.ROOT);


        Vista inicial = switch (rol) {
            case "ADMIN" -> Vista.ADMINISTRACION;
            case "TECNICO" -> Vista.ATENCION;
            case "EMPLEADO" -> Vista.TICKETS;
            default -> throw new IllegalArgumentException(
                    "El usuario no tiene un rol habilitado."
            );
        };


        if (sesionAutenticada == null
                || sesionAutenticada.usuarioId() != usuario.getId()) {
            throw new SecurityException(
                    "La identidad autenticada no corresponde al usuario."
            );
        }

        this.usuario = usuario;
        this.sesionAutenticada = sesionAutenticada;
        administrador = "ADMIN".equals(rol);
        tecnico = "TECNICO".equals(rol);
        supervision = administrador || tecnico;


        sesion.setText(
                usuario.getNombre() + " " + usuario.getApellido() + " · " + rol
        );


        habilitar(botonAdministracion, administrador);
        habilitar(botonTickets, administrador || "EMPLEADO".equals(rol));
        habilitar(botonHistorial, supervision);
        habilitar(botonAtencion, tecnico);
        habilitar(botonDashboard, supervision);


        mostrar(inicial);
    }


    private void habilitar(Button boton, boolean permitido) {
        if (boton == null) {
            return;
        }
        boton.setVisible(permitido);
        boton.setManaged(permitido);
        if (!boton.disableProperty().isBound()) {
            boton.setDisable(!permitido);
        }
    }


    @FXML
    private void abrirAdministracion() {
        mostrar(Vista.ADMINISTRACION);
    }


    @FXML
    private void abrirTickets() {
        mostrar(Vista.TICKETS);
    }


    @FXML
    private void abrirAtencion() {
        mostrar(Vista.ATENCION);
    }


    @FXML
    private void abrirHistorial() {
        mostrar(Vista.HISTORIAL);
    }


    @FXML
    private void abrirDashboard() {
        mostrar(Vista.DASHBOARD);
    }


    @FXML
    private void cerrarSesion() {
        try {
            if (sesionAutenticada != null) {
                sesionAutenticada.cerrar();
            }
            NautaTicketApp.mostrarBienvenida(
                    (Stage) sesion.getScene().getWindow()
            );
        } catch (IOException e) {
            mostrarError(
                    "No se pudo volver al inicio de sesión.",
                    e
            );
        }
    }


    private void mostrar(Vista vista) {
        if (usuario == null) {
            mensaje.setText("Debes iniciar sesión para continuar.");
            return;
        }


        boolean permitida = switch (vista) {
            case ADMINISTRACION -> administrador;
            case ATENCION -> tecnico;
            case DASHBOARD, HISTORIAL -> supervision;
            case TICKETS -> administrador
                    || "EMPLEADO".equals(usuario.getRol().getNombre());
        };


        if (!permitida) {
            mensaje.setText(
                    "El rol actual no tiene acceso a esta pantalla."
            );
            return;
        }


        try {
            FXMLLoader loader = new FXMLLoader(
                    NautaTicketApp.class.getResource(vista.archivo)
            );


            if (vista == Vista.TICKETS) {
                loader.setControllerFactory(tipo -> new TicketController(
                        HibernateUtil.getSessionFactory(),
                        usuario.getId(),
                        administrador
                ));
            }


            Parent pagina = loader.load();


            inicializarControlador(loader.getController());


            contenido.setContent(pagina);
            contenido.setVvalue(0);
            mensaje.setText("");


        } catch (IOException | RuntimeException e) {
            mostrarError(
                    "No se pudo abrir la pantalla. Revisa la conexión e inténtalo de nuevo.",
                    e
            );
        }
    }


    private void inicializarControlador(Object controlador) {


        switch (controlador) {


            case AdminController admin -> {
                if (!administrador) {
                    throw new SecurityException(
                            "Acceso administrativo no permitido."
                    );
                }
                admin.establecerAdministrador(usuario);
            }


            case AtencionController atencion -> {
                if (!tecnico) {
                    throw new SecurityException(
                            "Solo los técnicos TI pueden atender tickets."
                    );
                }
                atencion.setIdTecnico(usuario.getId());
                atencion.setServicio(new AtencionService(
                        HibernateUtil.getSessionFactory(), sesionAutenticada
                ));

                if (botonAtencion.disableProperty().isBound()) {
                    botonAtencion.disableProperty().unbind();
                }
                botonAtencion.disableProperty().bind(
                        atencion.tomaEnCurso()
                );
            }


            case HistorialController historial ->
                    historial.setHistorialService(
                            new HistorialService(
                                    HibernateUtil.getSessionFactory(),
                                    usuario.getId()
                            )
                    );


            case DashboardController dashboard -> {
                if (!supervision) {
                    throw new SecurityException(
                            "No tienes acceso a estadísticas."
                    );
                }


                dashboard.setHistorialService(
                        new HistorialService(
                                HibernateUtil.getSessionFactory(), usuario.getId()
                        )
                );

                if (administrador) {
                    dashboard.setTiemposResolucion(
                            HibernateUtil.getSessionFactory(), usuario.getId());
                }


                dashboard.setReportePdfAction(
                        (desde, hasta, estado) -> {
                            ReporteService servicio = new ReporteService(
                                    HibernateUtil.getSessionFactory(),
                                    usuario.getId()
                            );


                            return servicio.prepararReporte(
                                    desde,
                                    hasta,
                                    estado
                            );
                        }
                );


                dashboard.setApiDisponible(administrador);
            }


            case TicketController tickets -> {
            }


            default -> throw new IllegalStateException(
                    "La pantalla no tiene un controlador válido."
            );
        }
    }


    private void mostrarError(String texto, Exception error) {
        mensaje.setText(texto);


        System.getLogger(PanelController.class.getName()).log(
                System.Logger.Level.ERROR,
                texto,
                error
        );
    }
}
