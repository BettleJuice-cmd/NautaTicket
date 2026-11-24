package ni.edu.uam.nautaticket.service;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportesIntegracionTest {
    @Test
    void estadisticasProduceVistaPreviaYPdf() throws Exception {
        try (InputStream archivo = getClass().getResourceAsStream("/reports/reporte_estadisticas.jrxml")) {
            assertNotNull(archivo);
            var plantilla = JasperCompileManager.compileReport(archivo);
            var parametros = new java.util.HashMap<>(Map.<String, Object>of(
                    "PERIODO", "Período: Inicio al Actualidad | Estado: TODOS",
                    "FECHA_GENERACION", "23/11/2026 10:00",
                    "TOTAL_TICKETS", 12,
                    "PENDIENTES", 5,
                    "EN_PROCESO", 2,
                    "RESUELTOS", 4,
                    "CERRADOS", 1,
                    "CATEGORIA_1", "Software",
                    "CATEGORIA_1_TOTAL", 6,
                    "OBSERVACION", "Datos de prueba"));
            parametros.put("FONDO", FondoEstadisticas.imagen());
            var documento = JasperFillManager.fillReport(plantilla, parametros,
                    new JREmptyDataSource(1));
            assertFalse(documento.getPages().isEmpty());
            String html = ReporteService.crearVistaPrevia(documento);
            assertTrue(html.contains("Panorama"));
            assertTrue(html.contains("<img src=\"data:image/"));
            Files.writeString(Path.of("target", "reporte-estadisticas-vista.html"), html);
            byte[] pdf = JasperExportManager.exportReportToPdf(documento);
            assertTrue(pdf.length > 1000);
            Path muestra = Path.of("target", "reporte-estadisticas-muestra.pdf");
            Files.createDirectories(muestra.getParent());
            Files.write(muestra, pdf);
        }
    }

    @Test
    void historialProduceVistaPreviaYPdf() throws Exception {
        try (InputStream archivo = getClass().getResourceAsStream("/reports/reporte_movimientos.jrxml")) {
            assertNotNull(archivo);
            var plantilla = JasperCompileManager.compileReport(archivo);
            var parametros = new java.util.HashMap<>(Map.<String, Object>of(
                    "TOTAL_MOVIMIENTOS", 1,
                    "TOTAL_TICKETS", 1,
                    "CAMBIOS_ESTADO", 1,
                    "CAMBIOS_TECNICO", 0,
                    "FECHA_DESDE", "Sin límite",
                    "FECHA_HASTA", "Sin límite",
                    "ESTADO_FILTRO", "TODOS",
                    "FECHA_GENERACION", "23/11/2026 10:00"));
            var fila = new ReporteMovimientosService.FilaMovimiento(
                    1, 10, "Falla de red", "23/11/2026 09:00",
                    "Ana López", "CAMBIO DE ESTADO", "PENDIENTE", "EN PROCESO",
                    "Se inició la atención");
            var documento = JasperFillManager.fillReport(plantilla, parametros,
                    new JRMapCollectionDataSource(List.of(
                            ReporteMovimientosService.camposReporte(fila))));
            assertFalse(documento.getPages().isEmpty());
            assertTrue(ReporteService.crearVistaPrevia(documento).contains("Registro"));
            assertTrue(JasperExportManager.exportReportToPdf(documento).length > 1000);
        }
    }
}
