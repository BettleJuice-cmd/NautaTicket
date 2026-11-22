package ni.edu.uam.nautaticket.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.scene.web.WebEngine;
import ni.edu.uam.nautaticket.service.TiempoResolucionService;
import ni.edu.uam.nautaticket.service.HistorialService;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Estadísticas locales de resolución para el resumen administrativo. */
public class WebTiempoBridge {
    private final WebEngine engine;
    private final String paginaPermitida;
    private final int administradorId;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean activo = new AtomicBoolean(true);
    private boolean consultando;

    public WebTiempoBridge(WebEngine engine, String paginaPermitida, int administradorId) {
        this.engine = engine;
        this.paginaPermitida = paginaPermitida;
        this.administradorId = administradorId;
    }

    public void desactivar() {
        activo.set(false);
    }

    public void consultar() {
        if (!activo.get() || !paginaPermitida.equals(engine.getLocation()) || consultando) return;
        consultando = true;
        Task<Map<String, Object>> tarea = new Task<>() {
            @Override protected Map<String, Object> call() {
                List<Map<String, Object>> tecnicos = new TiempoResolucionService(
                        HibernateUtil.getSessionFactory(), administradorId)
                        .resumen().stream().map(t -> {
                            Map<String, Object> fila = new LinkedHashMap<>();
                            fila.put("nombre", t.nombre() + " (#" + t.id() + ")");
                            fila.put("promedio", t.promedioSegundos());
                            fila.put("cantidad", t.ticketsResueltos());
                            return fila;
                        }).toList();
                Map<String, Object> resultado = new LinkedHashMap<>();
                resultado.put("tecnicos", tecnicos);
                resultado.put("estados", new HistorialService(
                        HibernateUtil.getSessionFactory(), administradorId)
                        .totales(null, null, null));
                return resultado;
            }
        };
        tarea.setOnSucceeded(evento -> {
            consultando = false;
            enviar("nautaTiemposDataReady", tarea.getValue());
        });
        tarea.setOnFailed(evento -> {
            consultando = false;
            enviar("nautaTiemposDataError", "No se pudieron consultar los gráficos.");
        });
        Thread hilo = new Thread(tarea, "nautaticket-tiempos-web");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void enviar(String funcion, Object dato) {
        if (!activo.get() || !paginaPermitida.equals(engine.getLocation())) return;
        try {
            String json = mapper.writeValueAsString(dato);
            engine.executeScript("if(window." + funcion + "){window." + funcion
                    + "(JSON.parse(" + mapper.writeValueAsString(json) + "));}");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron mostrar los tiempos.", e);
        }
    }
}
