(function () {
    "use strict";

    const seccion = document.getElementById("historial");
    const puente = window.nautaHistorialBridge;

    if (!seccion || !puente) {
        return;
    }

    const estilos = document.createElement("style");

    estilos.textContent = `
        .historial-filtros {
            display: grid;
            grid-template-columns: 1fr 1fr 1fr 1.2fr;
            gap: 14px;
            padding: 22px;
            background: #0d0d0d;
            border: 1px solid #292929;
            border-radius: 9px;
        }

        .historial-campo {
            display: flex;
            flex-direction: column;
            gap: 9px;
            min-width: 0;
        }

        .historial-campo label {
            color: #b1b1b9;
            font-size: 12px;
        }

        .historial-input,
        .historial-select {
            width: 100%;
            min-height: 42px;
            box-sizing: border-box;
            padding: 10px 12px;
            color: #f2f2f4 !important;
            -webkit-text-fill-color: #f2f2f4;
            background: #080808 !important;
            border: 1px solid #353535;
            border-radius: 6px;
            outline: none;
            color-scheme: dark;
            font: 12px "IBM Plex Sans", "Segoe UI", sans-serif;
        }

        .historial-input::placeholder {
            color: #92929a;
            -webkit-text-fill-color: #92929a;
            opacity: 1;
        }

        .historial-input:focus,
        .historial-select:focus {
            border-color: #9783db;
            background: #0a0a0a !important;
        }

        .historial-select option {
            background: #111111;
            color: #f2f2f4;
        }

        .historial-acciones {
            grid-column: 1 / -1;
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 10px;
            margin-top: 5px;
        }

        .historial-resultado {
            color: #a0a0a9;
            font-size: 12px;
            margin-left: auto;
        }

        .historial-tabla {
            min-width: 780px;
        }

        .historial-tabla td {
            vertical-align: top;
            line-height: 1.6;
        }

        .historial-id {
            color: #b7a9e4;
            font-family: "IBM Plex Mono", monospace;
        }

        .historial-estado {
            display: inline-block;
            padding: 4px 8px;
            border: 1px solid #393346;
            border-radius: 5px;
            background: #1b1623;
            color: #c9baf4;
            font-size: 11px;
        }

        .historial-estado.vacio {
            color: #777780;
            background: transparent;
            border-color: transparent;
        }

        .historial-comentario {
            max-width: 390px;
            overflow-wrap: anywhere;
            color: #c6c6cf;
        }

        .historial-mensaje {
            margin: 17px 0;
            padding: 13px 16px;
            border: 1px solid #40304d;
            border-radius: 7px;
            color: #c9baf4;
            background: #141018;
            font-size: 12px;
        }

        .historial-mensaje.error {
            background: #1a1013;
            color: #e6a5ad;
            border-color: #4c2931;
        }

        .historial-tabla-vacia {
            padding: 40px 20px !important;
            color: #92929b;
            text-align: center;
        }

        @media (max-width: 1100px) {
            .historial-filtros {
                grid-template-columns: repeat(2, minmax(0,1fr));
            }
        }

        @media (max-width: 600px) {
            .historial-filtros {
                grid-template-columns: 1fr;
            }

            .historial-resultado {
                width: 100%;
                margin-left: 0;
            }
        }
    `;

    document.head.appendChild(estilos);

    seccion.innerHTML = `
        <div class="page-heading">
            <div>
                <h1>Historial de tickets</h1>
                <p>
                    Consulta los cambios y movimientos
                    registrados en NautaTicket.
                </p>
            </div>
        </div>

        <form id="historialFormulario"
              class="historial-filtros">

            <div class="historial-campo">
                <label for="historialTicketId">
                    ID del ticket
                </label>
                <input id="historialTicketId"
                       class="historial-input"
                       type="number"
                       min="1"
                       step="1"
                       placeholder="Todos los tickets">
            </div>

            <div class="historial-campo">
                <label for="historialDesde">
                    Desde
                </label>
                <input id="historialDesde"
                       class="historial-input"
                       type="date">
            </div>

            <div class="historial-campo">
                <label for="historialHasta">
                    Hasta
                </label>
                <input id="historialHasta"
                       class="historial-input"
                       type="date">
            </div>

            <div class="historial-campo">
                <label for="historialEstado">
                    Estado nuevo
                </label>
                <select id="historialEstado"
                        class="historial-select">
                    <option value="TODOS">Todos</option>
                    <option value="PENDIENTE">Pendiente</option>
                    <option value="EN_PROCESO">En proceso</option>
                    <option value="RESUELTO">Resuelto</option>
                    <option value="CERRADO">Cerrado</option>
                </select>
            </div>

            <div class="historial-acciones">
                <button id="historialBuscar"
                        class="button button-primary"
                        type="submit">
                    Buscar
                </button>

                <button id="historialLimpiar"
                        class="button button-secondary"
                        type="button">
                    Limpiar
                </button>

                <span id="historialContador"
                      class="historial-resultado">
                    Cargando movimientos...
                </span>
            </div>
        </form>

        <div id="historialMensaje"
             class="historial-mensaje"
             role="status"
             hidden></div>

        <div class="content-panel">
            <div class="panel-heading">
                <div>
                    <h2>Registro de movimientos</h2>
                    <p>
                        Los movimientos más recientes
                        se muestran primero.
                    </p>
                </div>
            </div>

            <div class="table-scroll">
                <table class="data-table historial-tabla">
                    <thead>
                        <tr>
                            <th>Ticket</th>
                            <th>Fecha</th>
                            <th>Usuario</th>
                            <th>Estado anterior</th>
                            <th>Estado nuevo</th>
                            <th>Comentario</th>
                        </tr>
                    </thead>

                    <tbody id="historialTabla">
                        <tr>
                            <td colspan="6"
                                class="historial-tabla-vacia">
                                Consultando historial...
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    `;

    const $ = id => document.getElementById(id);

    const formulario = $("historialFormulario");
    const ticketId = $("historialTicketId");
    const desde = $("historialDesde");
    const hasta = $("historialHasta");
    const estado = $("historialEstado");

    const buscarBtn = $("historialBuscar");
    const limpiarBtn = $("historialLimpiar");

    const tabla = $("historialTabla");
    const contador = $("historialContador");
    const mensajeElemento = $("historialMensaje");

    let consultando = false;

    function mensaje(texto, error = false) {
        mensajeElemento.textContent = texto;
        mensajeElemento.classList.toggle("error", error);
        mensajeElemento.hidden = !texto;
    }

    function agregarCelda(fila, valor) {
        const celda = document.createElement("td");
        celda.textContent =
            valor == null ? "" : String(valor);
        fila.appendChild(celda);
        return celda;
    }

    function vacio(texto) {
        tabla.replaceChildren();

        const fila = document.createElement("tr");
        const celda = agregarCelda(fila, texto);

        celda.colSpan = 6;
        celda.className = "historial-tabla-vacia";

        tabla.appendChild(fila);
    }

    function estadoCelda(fila, valor) {
        const celda = agregarCelda(fila, "");
        const etiqueta = document.createElement("span");

        etiqueta.className = "historial-estado";

        if (!valor) {
            etiqueta.textContent = "—";
            etiqueta.classList.add("vacio");
        } else {
            etiqueta.textContent = valor;
        }

        celda.appendChild(etiqueta);
    }

    function mostrarMovimientos(movimientos) {
        tabla.replaceChildren();

        contador.textContent =
            movimientos.length + " movimientos";

        if (!movimientos.length) {
            vacio(
                "No se encontraron movimientos con los filtros seleccionados."
            );
            return;
        }

        movimientos.forEach(movimiento => {
            const fila = document.createElement("tr");

            const id = agregarCelda(
                fila,
                movimiento.ticketId == null
                    ? "—"
                    : "#" + movimiento.ticketId
            );

            id.className = "historial-id";

            agregarCelda(fila, movimiento.fecha);
            agregarCelda(fila, movimiento.usuario);

            estadoCelda(fila, movimiento.estadoAnterior);
            estadoCelda(fila, movimiento.estadoNuevo);

            const comentario = agregarCelda(
                fila,
                movimiento.comentario || "—"
            );

            comentario.className = "historial-comentario";

            tabla.appendChild(fila);
        });
    }

    function consultar() {

        if (consultando) {
            return;
        }

        if (ticketId.value &&
            (!Number.isInteger(Number(ticketId.value))
                || Number(ticketId.value) <= 0)) {

            mensaje(
                "Ingresa un ID de ticket válido.",
                true
            );
            return;
        }

        if (desde.value &&
            hasta.value &&
            desde.value > hasta.value) {

            mensaje(
                "La fecha inicial no puede ser posterior a la final.",
                true
            );
            return;
        }

        consultando = true;

        buscarBtn.disabled = true;
        limpiarBtn.disabled = true;

        contador.textContent = "Consultando...";
        mensaje("");

        try {
            puente.buscar(
                ticketId.value,
                desde.value,
                hasta.value,
                estado.value
            );
        } catch (error) {
            consultando = false;
            buscarBtn.disabled = false;
            limpiarBtn.disabled = false;

            mensaje(
                "No se pudo iniciar la consulta del historial.",
                true
            );
        }
    }

    window.nautaHistorialDataReady = datos => {
        consultando = false;

        buscarBtn.disabled = false;
        limpiarBtn.disabled = false;

        const movimientos = Array.isArray(datos.movimientos)
            ? datos.movimientos
            : [];

        mostrarMovimientos(movimientos);
        mensaje("");
    };

    window.nautaHistorialDataError = texto => {
        consultando = false;

        buscarBtn.disabled = false;
        limpiarBtn.disabled = false;

        contador.textContent = "Consulta no disponible";

        mensaje(
            texto || "No se pudo consultar el historial.",
            true
        );
    };

    formulario.addEventListener("submit", evento => {
        evento.preventDefault();
        consultar();
    });

    limpiarBtn.addEventListener("click", () => {
        ticketId.value = "";
        desde.value = "";
        hasta.value = "";
        estado.value = "TODOS";

        consultar();
    });

    // Al ingresar a Historial, actualizar los datos.
    document.querySelectorAll(
        '[data-section="historial"]'
    ).forEach(boton => {
        boton.addEventListener("click", consultar);
    });

    // Primera consulta automática.
    consultar();

})();
