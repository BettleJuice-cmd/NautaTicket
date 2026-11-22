package ni.edu.uam.nautaticket.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Páginas vectoriales de gráficos para la vista previa y el PDF. */
final class ReporteGraficosTecnicos {
    private static final int POR_PAGINA = 8;
    private static final String VERDE = "#37B978";
    private static final String ROJO = "#DF6674";
    private static final String MORADO = "#9783DB";
    private static final String AMARILLO = "#D9A441";
    private static final String AZUL = "#5D91D6";
    private static final String GRIS = "#8792A1";

    private ReporteGraficosTecnicos() { }

    static void agregar(JasperPrint documento,
            List<TiempoResolucionService.ResumenTecnico> datos,
            Map<String, Long> estados) throws JRException {
        int paginas = Math.max(1, (datos.size() + POR_PAGINA - 1) / POR_PAGINA);
        for (int numero = 0; numero < paginas; numero++) {
            int inicio = numero * POR_PAGINA;
            int fin = Math.min(datos.size(), inicio + POR_PAGINA);
            String plantilla = plantilla(datos.subList(inicio, fin), datos, estados,
                    numero + 1, paginas);
            var reporte = JasperCompileManager.compileReport(
                    new ByteArrayInputStream(plantilla.getBytes(StandardCharsets.UTF_8)));
            JasperPrint graficos = JasperFillManager.fillReport(
                    reporte, new java.util.HashMap<>(), new JREmptyDataSource(1));
            documento.getPages().addAll(graficos.getPages());
        }
    }

    private static String plantilla(List<TiempoResolucionService.ResumenTecnico> pagina,
            List<TiempoResolucionService.ResumenTecnico> todos,
            Map<String, Long> estados,
            int numero, int paginas) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <jasperReport name="graficos_tiempos" language="java"
                    pageWidth="842" pageHeight="595" orientation="Landscape"
                    columnWidth="792" leftMargin="25" rightMargin="25"
                    topMargin="25" bottomMargin="25">
                  <title height="530">
                """);
        rectangulo(xml, 0, 0, 792, 53, "#213A5C");
        texto(xml, "NAUTATICKET - Estados y tiempos", 15, 9, 760, 23,
                17, true, "#FFFFFF");
        texto(xml, "Resumen de tickets y técnicos - página " + numero + " de " + paginas,
                15, 34, 760, 14, 9, false, "#FFFFFF");
        texto(xml, "Estado actual y promedio desde la asignación hasta la resolución. Datos globales, sin filtros de la tabla.",
                0, 62, 792, 18, 9, false, "#526174");
        graficoEstados(xml, estados, 90);
        graficoPromedio(xml, pagina, todos, 302);
        texto(xml, "Promedio: verde menor tiempo    Morado intermedio    Rojo mayor tiempo",
                0, 502, 680, 15, 8, false, "#526174");
        texto(xml, "NautaTicket - Estados y resolución", 0, 518, 792, 10,
                8, false, "#777777");
        xml.append("  </title>\n</jasperReport>\n");
        return xml.toString();
    }

    private static void graficoEstados(StringBuilder xml,
            Map<String, Long> estados, int y) {
        texto(xml, "Tickets por estado", 0, y, 792, 22,
                13, true, "#213A5C");
        String[][] opciones = {
                {"PENDIENTE", "Pendiente", AMARILLO},
                {"EN_PROCESO", "En proceso", AZUL},
                {"RESUELTO", "Resuelto", VERDE},
                {"CERRADO", "Cerrado", GRIS}
        };
        long maximo = Math.max(1, estados.values().stream()
                .mapToLong(Long::longValue).max().orElse(0));
        for (int indice = 0; indice < opciones.length; indice++) {
            String[] estado = opciones[indice];
            long total = estados.getOrDefault(estado[0], 0L);
            int filaY = y + 32 + indice * 25;
            texto(xml, estado[1], 0, filaY, 238, 17, 9, false, "#243348");
            rectangulo(xml, 245, filaY + 1, 380, 13, "#E7E9ED");
            if (total > 0) {
                rectangulo(xml, 245, filaY + 1,
                        Math.max(2, (int) Math.round(total / (double) maximo * 380)),
                        13, estado[2]);
            }
            texto(xml, total + (total == 1 ? " ticket" : " tickets"),
                    635, filaY, 157, 16, 9, false, "#243348");
        }
    }

    private static void graficoPromedio(StringBuilder xml,
            List<TiempoResolucionService.ResumenTecnico> pagina,
            List<TiempoResolucionService.ResumenTecnico> todos,
            int y) {
        texto(xml, "Tiempo promedio de todos sus tickets", 0, y, 792, 22,
                13, true, "#213A5C");
        if (pagina.isEmpty()) {
            texto(xml, "Aún no hay tickets resueltos.", 0, y + 42,
                    792, 18, 10, false, "#526174");
            return;
        }
        double menor = todos.stream().mapToDouble(
                        TiempoResolucionService.ResumenTecnico::promedioSegundos)
                .min().orElse(0);
        double mayor = todos.stream().mapToDouble(
                        TiempoResolucionService.ResumenTecnico::promedioSegundos)
                .max().orElse(0);
        double escala = Math.max(1, mayor);
        for (int indice = 0; indice < pagina.size(); indice++) {
            var tecnico = pagina.get(indice);
            double segundos = tecnico.promedioSegundos();
            int filaY = y + 32 + indice * 20;
            texto(xml, tecnico.nombre() + " (#" + tecnico.id() + ")",
                    0, filaY, 238, 16, 9, false, "#243348");
            rectangulo(xml, 245, filaY + 1, 380, 13, "#E7E9ED");
            String color = MORADO;
            if (menor < mayor) {
                if (Double.compare(segundos, menor) == 0) color = VERDE;
                else if (Double.compare(segundos, mayor) == 0) color = ROJO;
            }
            rectangulo(xml, 245, filaY + 1,
                    Math.max(2, (int) Math.round(segundos / escala * 380)), 13, color);
            texto(xml, TiempoResolucionService.formatear(Math.round(segundos)),
                    635, filaY, 157, 16, 9, false, "#243348");
        }
    }

    private static void rectangulo(StringBuilder xml, int x, int y,
            int ancho, int alto, String color) {
        xml.append("    <element kind=\"rectangle\" x=\"").append(x)
                .append("\" y=\"").append(y).append("\" width=\"")
                .append(ancho).append("\" height=\"").append(alto)
                .append("\" backcolor=\"").append(color)
                .append("\" mode=\"Opaque\"><pen lineWidth=\"0\"/></element>\n");
    }

    private static void texto(StringBuilder xml, String contenido,
            int x, int y, int ancho, int alto, int tamano,
            boolean negrita, String color) {
        xml.append("    <element kind=\"staticText\" x=\"").append(x)
                .append("\" y=\"").append(y).append("\" width=\"")
                .append(ancho).append("\" height=\"").append(alto)
                .append("\" fontSize=\"").append(tamano)
                .append("\" bold=\"").append(negrita)
                .append("\" forecolor=\"").append(color)
                .append("\"><text>").append(escaparXml(contenido))
                .append("</text></element>\n");
    }

    private static String escaparXml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
