package ni.edu.uam.nautaticket.service;

import ni.edu.uam.nautaticket.dto.ReporteTicketDTO;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporteDisenoTest {
    @Test
    void compilaYExportaInformeGeneralConGraficos() throws Exception {
        var tickets = new ArrayList<ReporteTicketDTO>();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 16, 9, 30);
        for (int i = 1; i <= 12; i++) {
            tickets.add(new ReporteTicketDTO(i, "Administración",
                    "Incidencia de prueba " + i, "Software", "Técnico " + (i % 3 + 1),
                    i % 3 == 0 ? "RESUELTO" : "PENDIENTE",
                    inicio.plusHours(i), i % 3 == 0 ? inicio.plusDays(i) : null));
        }
        Map<String, Long> estados = Map.of("PENDIENTE", 8L, "RESUELTO", 4L);
        var parametros = new HashMap<String, Object>();
        parametros.put("FECHA_DESDE", null);
        parametros.put("FECHA_HASTA", null);
        parametros.put("ESTADO_FILTRO", "TODOS");
        parametros.put("FECHA_GENERACION", inicio);
        parametros.put("TOTAL_TICKETS", tickets.size());
        parametros.put("TOTALES_ESTADO", estados);
        parametros.put("SIN_RESULTADOS", false);
        parametros.put("TITULO_REPORTE", "Informe general de soporte TI");

        try (InputStream plantilla = getClass().getResourceAsStream("/reports/reporte_tickets.jrxml")) {
            assertTrue(plantilla != null);
            var compilado = JasperCompileManager.compileReport(plantilla);
            var documento = JasperFillManager.fillReport(compilado, parametros,
                    new JRBeanCollectionDataSource(tickets));
            ReporteGraficosTecnicos.agregar(documento, List.of(
                    new TiempoResolucionService.ResumenTecnico(1, "Ana López", 3600, 3600, 3),
                    new TiempoResolucionService.ResumenTecnico(2, "Luis Pérez", 7200, 7200, 5),
                    new TiempoResolucionService.ResumenTecnico(3, "María Ruiz", 5400, 5400, 4)
            ), estados);
            assertTrue(documento.getPages().size() >= 2);
            Path salida = Path.of("target", "report-design-preview.pdf");
            Files.createDirectories(salida.getParent());
            JasperExportManager.exportReportToPdfFile(documento, salida.toString());
            assertTrue(Files.size(salida) > 1000);
        }
    }
}
