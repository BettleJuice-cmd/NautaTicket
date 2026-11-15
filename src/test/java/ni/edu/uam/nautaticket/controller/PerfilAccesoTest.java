package ni.edu.uam.nautaticket.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerfilAccesoTest {

    @Test
    void asignaCadaPerfilAlRolExistente() {
        assertTrue(PerfilAcceso.ADMINISTRADOR.coincideConRol("ADMIN"));
        assertTrue(PerfilAcceso.EMPLEADO.coincideConRol("EMPLEADO"));
        assertTrue(PerfilAcceso.PERSONAL_TI.coincideConRol("TECNICO"));
    }

    @Test
    void rechazaRolDiferenteYValoresInvalidos() {
        assertFalse(PerfilAcceso.ADMINISTRADOR.coincideConRol("TECNICO"));
        assertFalse(PerfilAcceso.EMPLEADO.coincideConRol("ADMIN"));
        assertFalse(PerfilAcceso.PERSONAL_TI.coincideConRol("EMPLEADO"));
        assertFalse(PerfilAcceso.PERSONAL_TI.coincideConRol(null));
        assertFalse(PerfilAcceso.PERSONAL_TI.coincideConRol(""));
    }

    @Test
    void admiteMayusculasYEspaciosDelRolPersistido() {
        assertTrue(PerfilAcceso.PERSONAL_TI.coincideConRol(" tecnico "));
        assertEquals("Personal TI", PerfilAcceso.PERSONAL_TI.getNombreVisible());
    }
}
