document.addEventListener("DOMContentLoaded", () => {

    const botonesNavegacion =
        document.querySelectorAll("[data-section]");

    const secciones =
        document.querySelectorAll(".workspace-section");

    const botonesIr =
        document.querySelectorAll("[data-go]");

    const elementosPorRol =
        document.querySelectorAll("[data-roles]");

    const currentSection =
        document.getElementById("currentSection");

    const userName =
        document.getElementById("userName");

    const userRole =
        document.getElementById("userRole");

    const roleTag =
        document.getElementById("roleTag");

    const cerrarSesionBtn =
        document.getElementById("cerrarSesionBtn");

    const botonesAdminTab =
        document.querySelectorAll("[data-admin-tab]");

    const nombresSecciones = {
        resumen: "Resumen",
        tickets: "Tickets",
        atencion: "Atención",
        historial: "Historial",
        reportes: "Reportes",
        administracion: "Administración"
    };

    let sesionConectada = false;
    let perfilActual = null;

    function rolPermitido(elemento) {

        const roles = elemento.dataset.roles;

        if (!roles) {
            return true;
        }

        if (!sesionConectada || !perfilActual) {
            return false;
        }

        const permitidos = roles
            .split(",")
            .map(rol => rol.trim());

        return permitidos.includes(perfilActual);
    }

    function aplicarPermisosVisuales() {

        elementosPorRol.forEach(elemento => {

            if (elemento.classList.contains(
                "workspace-section"
            )) {
                return;
            }

            elemento.hidden = !rolPermitido(elemento);
        });

        if (cerrarSesionBtn) {
            cerrarSesionBtn.hidden = !sesionConectada;
        }
    }

    function mostrarSeccion(nombre) {

        if (!Object.prototype.hasOwnProperty.call(
            nombresSecciones,
            nombre
        )) {
            return;
        }

        const seccionObjetivo =
            document.getElementById(nombre);

        if (!seccionObjetivo) {
            return;
        }

        if (!rolPermitido(seccionObjetivo)) {
            return;
        }

        secciones.forEach(seccion => {
            seccion.hidden = seccion.id !== nombre;
        });

        botonesNavegacion.forEach(boton => {

            const activo =
                boton.dataset.section === nombre;

            boton.classList.toggle(
                "active",
                activo
            );

            if (activo) {
                boton.setAttribute(
                    "aria-current",
                    "page"
                );
            } else {
                boton.removeAttribute(
                    "aria-current"
                );
            }
        });

        if (currentSection) {
            currentSection.textContent =
                nombresSecciones[nombre];
        }
    }

    function mostrarAdminTab(nombre) {

        if (perfilActual !== "Administrador") {
            return;
        }

        if (nombre !== "usuarios" &&
            nombre !== "departamentos") {
            return;
        }

        const usuarios =
            document.getElementById("adminUsuarios");

        const departamentos =
            document.getElementById("adminDepartamentos");

        if (usuarios) {
            usuarios.hidden = nombre !== "usuarios";
        }

        if (departamentos) {
            departamentos.hidden =
                nombre !== "departamentos";
        }

        botonesAdminTab.forEach(boton => {

            const activo =
                boton.dataset.adminTab === nombre;

            boton.classList.toggle(
                "active",
                activo
            );

            boton.setAttribute(
                "aria-selected",
                String(activo)
            );
        });
    }

    function mostrarDatosUsuario(correo, perfil) {

        if (userName) {
            userName.textContent = correo;
            userName.title = correo;
        }

        if (userRole) {
            userRole.textContent = perfil;
        }

        if (roleTag) {
            roleTag.textContent = perfil;
        }

        const avatar =
            document.querySelector(".user-avatar");

        if (avatar) {
            avatar.textContent =
                correo.charAt(0).toUpperCase();
        }
    }

    function mostrarVistaPrevia() {

        sesionConectada = false;
        perfilActual = null;

        if (userName) {
            userName.textContent =
                "Sin sesión conectada";
        }

        if (userRole) {
            userRole.textContent = "Vista previa";
        }

        if (roleTag) {
            roleTag.textContent = "Vista de diseño";
        }

        aplicarPermisosVisuales();
        mostrarSeccion("resumen");
    }

    botonesNavegacion.forEach(boton => {

        boton.addEventListener("click", () => {

            if (boton.hidden) {
                return;
            }

            mostrarSeccion(
                boton.dataset.section
            );
        });
    });

    botonesIr.forEach(boton => {

        boton.addEventListener("click", () => {
            mostrarSeccion(boton.dataset.go);
        });
    });

    botonesAdminTab.forEach(boton => {

        boton.addEventListener("click", () => {
            mostrarAdminTab(
                boton.dataset.adminTab
            );
        });
    });

    if (cerrarSesionBtn) {

        cerrarSesionBtn.addEventListener(
            "click",
            () => {

                if (!sesionConectada) {
                    return;
                }

                const puente =
                    window.nautaDashboardBridge;

                if (!puente) {
                    return;
                }

                try {
                    puente.cerrarSesion();

                } catch (error) {

                    console.error(
                        "Error cerrando sesión:",
                        error
                    );

                    window.alert(
                        "No fue posible cerrar sesión."
                    );
                }
            }
        );
    }

    // Se ejecuta cuando JavaFX registra
    // WebDashboardBridge.
    window.nautaDashboardReady = function () {

        const puente =
            window.nautaDashboardBridge;

        if (!puente) {
            mostrarVistaPrevia();
            return;
        }

        try {

            const correo = String(
                puente.getCorreo()
            ).trim();

            const perfil = String(
                puente.getPerfil()
            ).trim();

            const perfilesValidos = [
                "Administrador",
                "Soporte técnico",
                "Empleado"
            ];

            if (!correo ||
                !perfilesValidos.includes(perfil)) {

                mostrarVistaPrevia();
                return;
            }

            sesionConectada = true;
            perfilActual = perfil;

            mostrarDatosUsuario(correo, perfil);
            aplicarPermisosVisuales();
            mostrarSeccion("resumen");

            if (perfil === "Administrador") {
                mostrarAdminTab("usuarios");
            }

            console.log(
                "Dashboard preparado para:",
                perfil
            );

        } catch (error) {

            console.error(
                "No se pudo cargar la sesión:",
                error
            );

            mostrarVistaPrevia();
        }
    };

    mostrarVistaPrevia();

    // En WebView, Java invoca nautaDashboardReady
    // cuando termina de registrar el puente.
    if (window.nautaDashboardBridge) {
        window.nautaDashboardReady();
    }

});
