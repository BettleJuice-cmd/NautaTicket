package ni.edu.uam.nautaticket.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AtencionFXMLTest {
    @Test
    @Timeout(15)
    void cargaAtencionEInyectaIndicadorDeCapacidad() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX requiere una sesión gráfica.");
        var resultado = new CompletableFuture<FXMLLoader>();
        Platform.startup(() -> {
            try {
                var recurso = AtencionFXMLTest.class.getResource(
                        "/ni/edu/uam/nautaticket/atencion.fxml");
                assertNotNull(recurso);
                var loader = new FXMLLoader(recurso);
                loader.load();
                resultado.complete(loader);
            } catch (Throwable error) {
                resultado.completeExceptionally(error);
            }
        });
        try {
            var loader = resultado.get(10, TimeUnit.SECONDS);
            assertInstanceOf(AtencionController.class, loader.getController());
            var etiqueta = assertInstanceOf(Label.class,
                    loader.getNamespace().get("etiquetaCapacidad"));
            assertEquals("Mis tickets activos: -- / 3", etiqueta.getText());
        } finally {
            Platform.exit();
        }
    }
}
