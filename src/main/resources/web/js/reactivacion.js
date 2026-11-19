(function () {
    "use strict";

    if (window.nautaReactivacionInicializada) {
        return;
    }

    window.nautaReactivacionInicializada = true;

    const tabla = document.getElementById(
        "adminUsuariosBody"
    );

    if (!tabla) {
        return;
    }

    const estilos = document.createElement("style");

    estilos.textContent = `
        .admin-reactivar-button {
            color: #c9baf4 !important;
            background: #1b1526 !important;
            border: 1px solid #4b3b69 !important;
            border-radius: 5px;
            padding: 7px 12px;
            margin-left: 7px;
            font-size: 11px;
            cursor: pointer;
        }

        .admin-reactivar-button:hover {
            background: #30223e !important;
            border-color: #9783db !important;
        }

        .admin-reactivar-button:disabled {
            opacity: .5;
            cursor: not-allowed;
        }
    `;

    document.head.appendChild(estilos);

    function abrirConfirmacion(id, nombre) {

        const puente = window.nautaAdminBridge;

        if (!puente) {
            return;
        }

        const capa = document.querySelector(
            ".admin-modal-backdrop"
        );

        if (!capa || !capa.hidden) {
            return;
        }

        capa.replaceChildren();
        capa.hidden = false;

        const dialogo = document.createElement("div");

        dialogo.className = "admin-modal";
        dialogo.setAttribute("role", "dialog");
        dialogo.setAttribute("aria-modal", "true");
        dialogo.setAttribute(
            "aria-label",
            "Confirmar reactivación"
        );

        const titulo = document.createElement("h2");
        titulo.textContent = "Reactivar usuario";

        const descripcion = document.createElement("p");

        descripcion.textContent =
            "La cuenta volverá a estar activa y el usuario " +
            "podrá iniciar sesión con sus credenciales existentes.";

        const pregunta = document.createElement("p");

        pregunta.textContent =
            "¿Deseas reactivar a " + nombre + "?";

        const formulario = document.createElement("form");

        formulario.className = "admin-form";

        const estado = document.createElement("div");

        estado.className = "admin-modal-status";
        estado.setAttribute("role", "status");

        const acciones = document.createElement("div");

        acciones.className = "admin-modal-actions";

        const cancelar = document.createElement("button");

        cancelar.type = "button";
        cancelar.className = "button button-secondary";
        cancelar.textContent = "Cancelar";

        cancelar.addEventListener("click", () => {
            if (confirmar.disabled) {
                return;
            }

            capa.replaceChildren();
            capa.hidden = true;
        });

        const confirmar = document.createElement("button");

        confirmar.type = "submit";
        confirmar.className = "button button-primary";
        confirmar.textContent = "Reactivar usuario";

        acciones.append(cancelar, confirmar);
        formulario.append(estado, acciones);

        formulario.addEventListener("submit", evento => {

            evento.preventDefault();

            if (confirmar.disabled) {
                return;
            }

            confirmar.disabled = true;
            estado.textContent = "Reactivando usuario...";

            try {
                puente.ejecutarAccion(
                    "reactivarUsuario",
                    JSON.stringify({ id: id })
                );

            } catch (error) {

                confirmar.disabled = false;

                estado.textContent =
                    "No se pudo enviar la operación.";
            }
        });

        dialogo.append(
            titulo,
            descripcion,
            pregunta,
            formulario
        );

        capa.appendChild(dialogo);
        confirmar.focus();
    }

    function incorporarBotones() {

        const filas = Array.from(tabla.querySelectorAll("tr"));

        filas.forEach(fila => {

            const celdas = fila.querySelectorAll("td");

            // La tabla tiene 7 columnas:
            // ID, usuario, correo, rol,
            // departamento, estado y acciones.

            if (celdas.length !== 7) {
                return;
            }

            const estado = celdas[5].textContent.trim();

            if (estado !== "Inactivo") {
                return;
            }

            const id = Number(
                celdas[0].textContent.trim()
            );

            if (!Number.isInteger(id) || id <= 0) {
                return;
            }

            const acciones = celdas[6];

            if (acciones.querySelector(
                ".admin-reactivar-button"
            )) {
                return;
            }

            const nombre = celdas[1].textContent.trim();

            const boton = document.createElement("button");

            boton.type = "button";
            boton.className = "admin-reactivar-button";
            boton.textContent = "Reactivar";

            boton.addEventListener("click", () => {

                const seccion = document.getElementById(
                    "administracion"
                );

                if (!seccion || seccion.hidden
                    || !window.nautaAdminBridge) {
                    return;
                }

                abrirConfirmacion(id, nombre);
            });

            acciones.appendChild(boton);
        });
    }

    // La tabla se actualiza al buscar, filtrar
    // o completar una operación administrativa.
    // Observamos esos cambios sin sustituir
    // las funciones existentes de dashboard.js.

    const observador = new MutationObserver(() => {
        incorporarBotones();
    });

    observador.observe(tabla, {
        childList: true,
        subtree: true
    });

    incorporarBotones();

    // Si la operación falla, permitir reintentar
    // desde la misma ventana de confirmación.

    const resultadoOriginal =
        window.nautaAdminActionResult;

    if (typeof resultadoOriginal === "function") {

        window.nautaAdminActionResult = resultado => {

            resultadoOriginal(resultado);

            if (!resultado || resultado.correcto) {
                return;
            }

            const capa = document.querySelector(
                ".admin-modal-backdrop"
            );

            if (!capa || capa.hidden) {
                return;
            }

            const boton = capa.querySelector(
                'button[type="submit"]'
            );

            if (boton) {
                boton.disabled = false;
            }
        };
    }

})();
