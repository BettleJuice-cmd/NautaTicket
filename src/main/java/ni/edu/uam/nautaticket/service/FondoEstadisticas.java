package ni.edu.uam.nautaticket.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

/** Fondo único para evitar uniones visibles entre figuras en visores PDF. */
final class FondoEstadisticas {
    private static final int ESCALA = 3;
    private static final Image FONDO = dibujar();

    private FondoEstadisticas() { }

    static Image imagen() {
        return FONDO;
    }

    private static Image dibujar() {
        BufferedImage imagen = new BufferedImage(792 * ESCALA, 510 * ESCALA,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        try {
            g.scale(ESCALA, ESCALA);
            bloque(g, 0, 0, 792, 510, "#FFFFFF");
            bloque(g, 0, 0, 792, 98, "#121521");
            bloque(g, 0, 0, 7, 98, "#9B84D9");
            bloque(g, 648, 24, 118, 45, "#282439");

            int[] columnas = {0, 202, 404, 606};
            String[] acentos = {"#8A73CD", "#8A73CD", "#4679AD", "#23847B"};
            for (int i = 0; i < columnas.length; i++) {
                bloque(g, columnas[i], 114, 186, 100, "#F2F3F7");
                bloque(g, columnas[i], 114, 186, 4, acentos[i]);
            }
            bloque(g, 0, 230, 485, 235, "#F8F9FB");
            bloque(g, 19, 283, 447, 1, "#E1E4EC");
            bloque(g, 501, 230, 291, 235, "#171B2B");
            bloque(g, 0, 481, 792, 1, "#DEE1E8");
        } finally {
            g.dispose();
        }
        return imagen;
    }

    private static void bloque(Graphics2D g, int x, int y, int ancho, int alto,
            String color) {
        g.setColor(Color.decode(color));
        g.fillRect(x, y, ancho, alto);
    }
}
