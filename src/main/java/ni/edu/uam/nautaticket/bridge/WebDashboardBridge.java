
package ni.edu.uam.nautaticket.bridge;

import javafx.scene.web.WebEngine;

public class WebDashboardBridge {

    private final WebEngine engine;
    private final String paginaPermitida;

    private final String correo;
    private final String perfil;

    private final Runnable alCerrarSesion;

    public WebDashboardBridge(
            WebEngine engine,
            String paginaPermitida,
            String correo,
            String perfil,
            Runnable alCerrarSesion
    ) {
        this.engine = engine;
        this.paginaPermitida = paginaPermitida;
        this.correo = correo;
        this.perfil = perfil;
        this.alCerrarSesion = alCerrarSesion;
    }

    public String getCorreo() {

        if (!paginaAutorizada()) {
            return "";
        }

        return correo;
    }

    public String getPerfil() {

        if (!paginaAutorizada()) {
            return "";
        }

        return perfil;
    }

    public void cerrarSesion() {

        if (!paginaAutorizada()) {
            return;
        }

        alCerrarSesion.run();
    }

    private boolean paginaAutorizada() {

        return paginaPermitida.equals(
                engine.getLocation()
        );
    }
}
