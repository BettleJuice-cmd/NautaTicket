package ni.edu.uam.nautaticket.controller;

import java.time.LocalDate;
import net.sf.jasperreports.engine.JasperPrint;

/** Adaptador de UI, no implementa JasperReports ni accede a la base de datos. */
@FunctionalInterface
public interface ReportePdfAction {
    /** estado = null significa todos los estados. Fechas nulas: sin límite. */
    JasperPrint preparar(LocalDate desde, LocalDate hasta, String estado) throws Exception;
}
