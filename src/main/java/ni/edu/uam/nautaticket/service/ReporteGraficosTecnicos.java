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
    private static final String VERDE = "#16A58A";
    private static final String ROJO = "#D46A68";
    private static final String MORADO = "#7489BC";
    private static final String AMARILLO = "#DDA949";
    private static final String AZUL = "#5C9BD1";
    private static final String GRIS = "#8D78B8";
    private static final String TINTA = "#152A3D";
    private static final String TEXTO = "#526477";
    private static final String FONDO = "#F1F5F8";

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
        rectangulo(xml, 0, 0, 792, 62, TINTA);
        rectangulo(xml, 0, 0, 6, 62, VERDE);
        texto(xml, "NAUTATICKET / ANÁLISIS", 20, 8, 750, 16,
                9, true, "#77D5D0");
        texto(xml, "Estados y tiempos de resolución", 20, 27, 750, 27,
                19, true, "#FFFFFF");
        texto(xml, "Datos globales del sistema · Página " + numero + " de " + paginas,
                2, 70, 785, 17, 9, false, TEXTO);
        rectangulo(xml, 0, 98, 792, 184, FONDO);
        graficoEstados(xml, estados, 111);
        rectangulo(xml, 0, 294, 792, 210, FONDO);
        graficoPromedio(xml, pagina, todos, 307);
        texto(xml, "NautaTicket · Promedio medido desde la asignación hasta la resolución",
                0, 517, 792, 12, 8, false, TEXTO);
        xml.append("  </title>\n</jasperReport>\n");
        return xml.toString();
    }

    private static void graficoEstados(StringBuilder xml,
            Map<String, Long> estados, int y) {
        texto(xml, "01  Tickets por estado", 14, y, 760, 22,
                13, true, TINTA);
        texto(xml, "Cantidad y porcentaje de todos los tickets", 14, y + 22,
                760, 14, 8, false, TEXTO);
        String[][] opciones = {
                {"PENDIENTE", "Pendiente", AMARILLO},
                {"EN_PROCESO", "En proceso", AZUL},
                {"RESUELTO", "Resuelto", VERDE},
                {"CERRADO", "Cerrado", GRIS}
        };
        long totalGeneral = estados.values().stream().mapToLong(Long::longValue).sum();
        long maximo = Math.max(1, estados.values().stream()
                .mapToLong(Long::longValue).max().orElse(0));
        for (int indice = 0; indice < opciones.length; indice++) {
            String[] estado = opciones[indice];
            long total = estados.getOrDefault(estado[0], 0L);
            int filaY = y + 45 + indice * 27;
            texto(xml, estado[1], 14, filaY, 205, 18, 9, true, TINTA);
            rectangulo(xml, 220, filaY + 3, 345, 12, "#DCE5EB");
            if (total > 0) {
                rectangulo(xml, 220, filaY + 3,
                        Math.max(3, (int) Math.round(total / (double) maximo * 345)),
                        12, estado[2]);
            }
            texto(xml, total + (total == 1 ? " ticket" : " tickets"),
                    580, filaY, 100, 18, 9, true, TINTA);
            long porcentaje = totalGeneral == 0 ? 0
                    : Math.round(total * 100.0 / totalGeneral);
            texto(xml, porcentaje + "% del total", 684, filaY, 95, 18,
                    9, false, TEXTO);
        }
    }

    private static void graficoPromedio(StringBuilder xml,
            List<TiempoResolucionService.ResumenTecnico> pagina,
            List<TiempoResolucionService.ResumenTecnico> todos,
            int y) {
        texto(xml, "02  Tiempo promedio por técnico", 14, y, 760, 22,
                13, true, TINTA);
        texto(xml, "Cada barra muestra el tiempo medio de sus tickets resueltos; escala relativa al mayor promedio.",
                14, y + 22, 760, 14, 8, false, TEXTO);
        if (pagina.isEmpty()) {
            texto(xml, "Aún no hay tickets resueltos para calcular promedios.",
                    14, y + 62, 760, 18, 10, false, TEXTO);
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
            int filaY = y + 45 + indice * 18;
            texto(xml, tecnico.nombre() + " (#" + tecnico.id() + ")",
                    14, filaY, 205, 16, 8, false, TINTA);
            rectangulo(xml, 220, filaY + 2, 345, 11, "#DCE5EB");
            String color = MORADO;
            if (menor < mayor) {
                if (Double.compare(segundos, menor) == 0) color = VERDE;
                else if (Double.compare(segundos, mayor) == 0) color = ROJO;
            }
            rectangulo(xml, 220, filaY + 2,
                    Math.max(3, (int) Math.round(segundos / escala * 345)), 11, color);
            texto(xml, TiempoResolucionService.formatear(Math.round(segundos)),
                    580, filaY, 195, 16, 9, true, TINTA);
        }
        texto(xml, menor < mayor
                        ? "Verde: menor promedio   Azul: intermedio   Coral: mayor promedio"
                        : "Todos los técnicos tienen el mismo promedio.",
                14, 486, 760, 14, 8, false, TEXTO);
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
                .append("\" fontName=\"Segoe UI")
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
