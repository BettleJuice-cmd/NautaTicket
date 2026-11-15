package ni.edu.uam.nautaticket.controller;

import java.nio.file.Path;
import java.time.LocalDate;

/** Adaptador de UI, no implementa JasperReports ni accede a la base de datos. */
@FunctionalInterface
public interface ReportePdfAction {
    /** estado = null significa todos los estados. Fechas nulas: sin límite. */
    void generar(Path destino, LocalDate desde, LocalDate hasta, String estado) throws Exception;
}
