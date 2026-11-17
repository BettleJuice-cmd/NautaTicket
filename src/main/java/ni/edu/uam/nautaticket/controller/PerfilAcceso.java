package ni.edu.uam.nautaticket.controller;

public enum PerfilAcceso {
    ADMINISTRADOR("Administrador", "ADMIN"),
    EMPLEADO("Empleado", "EMPLEADO"),
    PERSONAL_TI("Personal TI", "TECNICO");

    private final String nombreVisible;
    private final String rolBaseDatos;

    PerfilAcceso(String nombreVisible, String rolBaseDatos) {
        this.nombreVisible = nombreVisible;
        this.rolBaseDatos = rolBaseDatos;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public boolean coincideConRol(String rolReal) {
        return rolReal != null && rolBaseDatos.equalsIgnoreCase(rolReal.trim());
    }
}
