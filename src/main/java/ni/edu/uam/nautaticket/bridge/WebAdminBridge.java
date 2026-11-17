
package ni.edu.uam.nautaticket.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.scene.web.WebEngine;
import ni.edu.uam.nautaticket.model.Departamento;
import ni.edu.uam.nautaticket.model.Rol;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.AdministracionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebAdminBridge {

    private static final Logger LOGGER =
            Logger.getLogger(WebAdminBridge.class.getName());

    private final WebEngine engine;
    private final String paginaPermitida;
    private final Usuario administrador;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicBoolean activo =
            new AtomicBoolean(true);

    private boolean procesando = false;

    public WebAdminBridge(
            WebEngine engine,
            String paginaPermitida,
            Usuario administrador
    ) {
        this.engine = engine;
        this.paginaPermitida = paginaPermitida;
        this.administrador = administrador;
    }

    public void desactivar() {
        activo.set(false);
    }

    public void listarDatos() {

        if (!paginaAutorizada() || procesando) {
            return;
        }

        procesando = true;

        Task<Map<String, Object>> tarea = new Task<>() {

            @Override
            protected Map<String, Object> call() {
                verificarActivo();

                AdministracionService servicio =
                        new AdministracionService(administrador);

                return construirDatos(servicio);
            }
        };

        tarea.setOnSucceeded(event -> {

            procesando = false;

            if (!paginaAutorizada()) {
                return;
            }

            enviarRespuesta(
                    "nautaAdminDataReady",
                    tarea.getValue()
            );
        });

        tarea.setOnFailed(event -> {

            procesando = false;

            LOGGER.log(
                    Level.SEVERE,
                    "No se pudieron consultar los datos",
                    tarea.getException()
            );

            enviarErrorConsulta(
                    "No se pudieron consultar los datos de administración."
            );
        });

        iniciarTarea(tarea, "nautaticket-admin-consulta");
    }

    public void ejecutarAccion(
            String accion,
            String datosJson
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        if (procesando) {
            enviarResultado(
                    false,
                    "Hay una operación en proceso."
            );
            return;
        }

        if (accion == null || datosJson == null
                || datosJson.length() > 16384) {

            enviarResultado(false, "Solicitud inválida.");
            return;
        }

        procesando = true;

        Task<String> tarea = new Task<>() {

            @Override
            protected String call() throws Exception {

                verificarActivo();

                AdministracionService servicio =
                        new AdministracionService(administrador);

                JsonNode datos = mapper.readTree(datosJson);

                if (datos == null || !datos.isObject()) {
                    throw new IllegalArgumentException(
                            "Los datos enviados no son válidos."
                    );
                }

                return ejecutarOperacion(
                        servicio,
                        accion,
                        datos
                );
            }
        };

        tarea.setOnSucceeded(event -> {

            procesando = false;

            if (!paginaAutorizada()) {
                return;
            }

            enviarResultado(true, tarea.getValue());
        });

        tarea.setOnFailed(event -> {

            procesando = false;

            Throwable error = tarea.getException();

            LOGGER.log(
                    Level.SEVERE,
                    "Error ejecutando operación administrativa",
                    error
            );

            String mensaje = error instanceof IllegalArgumentException
                    ? error.getMessage()
                    : "No se pudo completar la operación.";

            enviarResultado(false, mensaje);
        });

        iniciarTarea(tarea, "nautaticket-admin-operacion");
    }

    private String ejecutarOperacion(
            AdministracionService servicio,
            String accion,
            JsonNode datos
    ) {

        return switch (accion) {

            case "crearUsuario" -> {

                Rol rol = buscarRol(
                        servicio,
                        entero(datos, "rolId")
                );

                Departamento departamento =
                        buscarDepartamento(
                                servicio,
                                entero(datos, "departamentoId")
                        );

                servicio.registrarUsuario(
                        texto(datos, "nombre"),
                        texto(datos, "apellido"),
                        texto(datos, "correo"),
                        texto(datos, "password"),
                        rol,
                        departamento
                );

                yield "Usuario registrado correctamente.";
            }

            case "editarUsuario" -> {

                Rol rol = buscarRol(
                        servicio,
                        entero(datos, "rolId")
                );

                Departamento departamento =
                        buscarDepartamento(
                                servicio,
                                entero(datos, "departamentoId")
                        );

                servicio.actualizarUsuario(
                        entero(datos, "id"),
                        texto(datos, "nombre"),
                        texto(datos, "apellido"),
                        texto(datos, "correo"),
                        texto(datos, "password"),
                        rol,
                        departamento
                );

                yield "Usuario actualizado correctamente.";
            }

            case "desactivarUsuario" -> {

                servicio.desactivarUsuario(
                        entero(datos, "id")
                );

                yield "Usuario desactivado correctamente.";
            }

            case "crearDepartamento" -> {

                servicio.crearDepartamento(
                        texto(datos, "nombre"),
                        texto(datos, "descripcion")
                );

                yield "Departamento creado correctamente.";
            }

            default -> throw new IllegalArgumentException(
                    "Operación no permitida."
            );
        };
    }

    private Map<String, Object> construirDatos(
            AdministracionService servicio
    ) {

        List<Map<String, Object>> usuarios =
                new ArrayList<>();

        List<Map<String, Object>> roles =
                new ArrayList<>();

        List<Map<String, Object>> departamentos =
                new ArrayList<>();

        int usuariosActivos = 0;

        for (Rol rol : servicio.listarRoles()) {

            Map<String, Object> fila =
                    new LinkedHashMap<>();

            fila.put("id", rol.getId());
            fila.put("nombre", rol.getNombre());

            roles.add(fila);
        }

        for (Departamento departamento
                : servicio.listarDepartamentos()) {

            Map<String, Object> fila =
                    new LinkedHashMap<>();

            fila.put("id", departamento.getId());
            fila.put("nombre", departamento.getNombre());

            departamentos.add(fila);
        }

        for (Usuario usuario : servicio.listarUsuarios()) {

            Map<String, Object> fila =
                    new LinkedHashMap<>();

            fila.put("id", usuario.getId());
            fila.put("nombre", usuario.getNombre());
            fila.put("apellido", usuario.getApellido());
            fila.put("correo", usuario.getCorreo());

            fila.put(
                    "rol",
                    usuario.getRol() == null
                            ? "Sin rol"
                            : usuario.getRol().getNombre()
            );

            fila.put(
                    "rolId",
                    usuario.getRol() == null
                            ? null
                            : usuario.getRol().getId()
            );

            fila.put(
                    "departamento",
                    usuario.getDepartamento() == null
                            ? "Sin departamento"
                            : usuario.getDepartamento().getNombre()
            );

            fila.put(
                    "departamentoId",
                    usuario.getDepartamento() == null
                            ? null
                            : usuario.getDepartamento().getId()
            );

            fila.put("activo", usuario.isActivo());

            if (usuario.isActivo()) {
                usuariosActivos++;
            }

            usuarios.add(fila);
        }

        Map<String, Object> resultado =
                new LinkedHashMap<>();

        resultado.put("usuarios", usuarios);
        resultado.put("roles", roles);
        resultado.put("departamentos", departamentos);

        resultado.put("totalUsuarios", usuarios.size());
        resultado.put("usuariosActivos", usuariosActivos);
        resultado.put(
                "totalDepartamentos",
                departamentos.size()
        );

        return resultado;
    }

    private Rol buscarRol(
            AdministracionService servicio,
            Integer id
    ) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Selecciona un rol."
            );
        }

        return servicio.listarRoles()
                .stream()
                .filter(rol -> id.equals(rol.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El rol seleccionado no existe."
                        )
                );
    }

    private Departamento buscarDepartamento(
            AdministracionService servicio,
            Integer id
    ) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Selecciona un departamento."
            );
        }

        return servicio.listarDepartamentos()
                .stream()
                .filter(departamento ->
                        id.equals(departamento.getId())
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El departamento seleccionado no existe."
                        )
                );
    }

    private String texto(JsonNode datos, String campo) {

        JsonNode valor = datos.get(campo);

        if (valor == null || valor.isNull()) {
            return "";
        }

        if (!valor.isTextual()) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " no es válido."
            );
        }

        return valor.asText();
    }

    private Integer entero(JsonNode datos, String campo) {

        JsonNode valor = datos.get(campo);

        if (valor == null || valor.isNull()) {
            return null;
        }

        if (!valor.isIntegralNumber() || !valor.canConvertToInt()) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " no es válido."
            );
        }

        return valor.intValue();
    }

    private void enviarResultado(
            boolean correcto,
            String mensaje
    ) {

        Map<String, Object> resultado =
                new LinkedHashMap<>();

        resultado.put("correcto", correcto);
        resultado.put("mensaje", mensaje);

        enviarRespuesta(
                "nautaAdminActionResult",
                resultado
        );
    }

    private void enviarErrorConsulta(String mensaje) {

        if (!paginaAutorizada()) {
            return;
        }

        enviarRespuesta(
                "nautaAdminDataError",
                mensaje
        );
    }

    private void enviarRespuesta(
            String funcion,
            Object datos
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        try {

            String json = mapper.writeValueAsString(datos);

            // JSON.parse recibe una cadena escapada.
            // No concatenamos datos de BD como código JS.
            String argumento =
                    mapper.writeValueAsString(json);

            engine.executeScript(
                    "if (typeof window." + funcion +
                            " === 'function') {" +
                            "window." + funcion +
                            "(JSON.parse(" + argumento + "));" +
                            "}"
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Error enviando respuesta a WebView",
                    e
            );
        }
    }

    private void verificarActivo() {

        if (!activo.get()) {
            throw new SecurityException(
                    "La sesión ya no está disponible."
            );
        }
    }

    private boolean paginaAutorizada() {

        return activo.get()
                && paginaPermitida.equals(engine.getLocation())
                && administrador != null
                && administrador.getId() != null;
    }

    private void iniciarTarea(
            Task<?> tarea,
            String nombre
    ) {

        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }
}
