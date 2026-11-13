BEGIN;

-- Mantiene estables los datos revisados hasta que se cree el índice.
LOCK TABLE tickets, asignaciones_ticket IN SHARE ROW EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT id_ticket
        FROM asignaciones_ticket
        WHERE fecha_fin IS NULL
        GROUP BY id_ticket
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Hay tickets con más de una asignación activa'
            USING HINT = 'Revise los duplicados antes de ejecutar de nuevo la migración 05.';
    END IF;
END;
$$;

-- La migración 04 pudo dejar abiertas asignaciones de tickets ya finalizados.
WITH finalizadas AS (
    SELECT a.id_asignacion,
           CASE WHEN e.nombre = 'CERRADO'
                THEN COALESCE(t.fecha_resolucion, t.fecha_cierre)
                ELSE t.fecha_resolucion
           END AS fecha_fin
    FROM asignaciones_ticket a
    JOIN tickets t ON t.id_ticket = a.id_ticket AND t.id_tecnico = a.id_tecnico
    JOIN estados_ticket e ON e.id_estado = t.id_estado
    WHERE a.fecha_fin IS NULL AND e.nombre IN ('RESUELTO', 'CERRADO')
)
UPDATE asignaciones_ticket a
SET fecha_fin = f.fecha_fin
FROM finalizadas f
WHERE a.id_asignacion = f.id_asignacion
  AND f.fecha_fin >= a.fecha_asignacion;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM asignaciones_ticket a
        JOIN tickets t ON t.id_ticket = a.id_ticket
        JOIN estados_ticket e ON e.id_estado = t.id_estado
        WHERE a.fecha_fin IS NULL AND e.nombre IN ('RESUELTO', 'CERRADO')
    ) THEN
        RAISE EXCEPTION 'Hay asignaciones abiertas de tickets finalizados que requieren revisión'
            USING HINT = 'Compruebe el técnico y las fechas de asignación, resolución y cierre.';
    END IF;
END;
$$;

CREATE UNIQUE INDEX ux_asignaciones_ticket_activa
    ON asignaciones_ticket (id_ticket)
    WHERE fecha_fin IS NULL;

COMMIT;
