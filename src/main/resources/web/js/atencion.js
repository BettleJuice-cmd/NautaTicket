
(function () {
    "use strict";

    const puente = window.nautaAtencionBridge;
    const seccion = document.getElementById("atencion");

    if (!puente || !seccion) {
        return;
    }

    const $ = id => document.getElementById(id);

    let pendientes = [];
    let asignados = [];
    let seleccionadoId = null;

    let ticketsActivos = 0;
    let capacidadMaxima = 0;
    let capacidadDisponible = false;

    let consultando = false;
    let operando = false;

    const estilos = document.createElement("style");

    estilos.textContent = `
        .atencion-metricas {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 14px;
            margin-bottom: 20px;
        }

        .atencion-metrica {
            padding: 20px;
            background: #0d0d0d;
            border: 1px solid #292929;
            border-radius: 9px;
        }

        .atencion-metrica span {
            display: block;
            color: #a5a5ad;
            font-size: 12px;
        }

        .atencion-metrica strong {
            display: block;
            margin-top: 13px;
            color: #f2f2f4;
            font-family: "IBM Plex Mono", Consolas, monospace;
            font-size: 26px;
            font-weight: 500;
        }

        .atencion-capacidad {
            padding: 20px;
            margin-bottom: 22px;
            background: #0d0d0d;
            border: 1px solid #292929;
            border-radius: 9px;
        }

        .atencion-capacidad-cabecera {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            margin-bottom: 17px;
        }

        .atencion-capacidad-cabecera h2 {
            margin: 0 0 6px;
            font-size: 14px;
            font-weight: 600;
        }

        .atencion-capacidad-cabecera p {
            margin: 0;
            color: #a2a2aa;
            font-size: 12px;
        }

        .atencion-capacidad-valor {
            color: #c9baf4;
            font-family: "IBM Plex Mono", Consolas, monospace;
            font-size: 21px;
        }

        .atencion-capacidad-barra {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 8px;
        }

        .atencion-capacidad-segmento {
            height: 9px;
            background: #292929;
            border-radius: 5px;
        }

        .atencion-capacidad-segmento.ocupado {
            background: #9783db;
        }

        .atencion-capacidad-segmento.lleno {
            background: #cf869a;
        }

        .atencion-capacidad-aviso {
            margin-top: 15px;
            color: #a9a9b1;
            font-size: 12px;
            line-height: 1.6;
        }

        .atencion-capacidad-aviso.alerta {
            color: #e0a6ae;
        }

        .atencion-panel {
            margin-top: 20px;
            overflow: hidden;
            background: #0d0d0d;
            border: 1px solid #292929;
            border-radius: 9px;
        }

        .atencion-panel-cabecera {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            justify-content: space-between;
            gap: 14px;
            padding: 20px 22px;
            border-bottom: 1px solid #292929;
        }

        .atencion-panel-cabecera h2 {
            margin: 0 0 7px;
            font-size: 15px;
            font-weight: 600;
        }

        .atencion-panel-cabecera p {
            margin: 0;
            color: #9999a3;
            font-size: 12px;
            line-height: 1.6;
        }

        .atencion-botones {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
        }

        .atencion-tabla-contenedor {
            overflow-x: auto;
        }

        .atencion-tabla {
            width: 100%;
            min-width: 580px;
            border-collapse: collapse;
            text-align: left;
        }

        .atencion-tabla th {
            padding: 15px 20px;
            color: #a3a3ac;
            background: #141414;
            font-size: 11px;
            font-weight: 500;
        }

        .atencion-tabla td {
            padding: 17px 20px;
            color: #e7e7eb;
            border-top: 1px solid #252525;
            font-size: 12px;
        }

        .atencion-tabla tr.seleccionable {
            cursor: pointer;
        }

        .atencion-tabla tr.seleccionable:hover td {
            background: #17141c;
        }

        .atencion-tabla tr.seleccionado td {
            background: #21192d;
        }

        .atencion-tabla tr.seleccionable:focus-visible {
            outline: 2px solid #9783db;
            outline-offset: -2px;
        }

        .atencion-vacio {
            padding: 35px 20px !important;
            color: #92929b !important;
            text-align: center;
        }

        .atencion-estado {
            display: inline-block;
            padding: 5px 9px;
            border-radius: 5px;
            color: #dbc38f;
            background: #241c11;
            border: 1px solid #55462b;
            font-size: 11px;
        }

        .atencion-estado.proceso {
            color: #a8bfed;
            background: #131a28;
            border-color: #34445b;
        }

        .atencion-detalle {
            padding: 21px 22px;
            border-top: 1px solid #292929;
        }

        .atencion-detalle h3 {
            margin: 0 0 11px;
            font-size: 13px;
            font-weight: 600;
        }

        .atencion-detalle p {
            color: #a5a5ad;
            font-size: 12px;
            line-height: 1.7;
        }

        .atencion-solucion {
            display: block;
            box-sizing: border-box;
            width: 100%;
            min-height: 105px;
            margin: 12px 0 16px;
            padding: 12px;
            color: #f2f2f4 !important;
            -webkit-text-fill-color: #f2f2f4;
            background: #080808 !important;
            border: 1px solid #333333;
            border-radius: 6px;
            font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
            font-size: 12px;
            resize: vertical;
            color-scheme: dark;
            outline: none;
        }

        .atencion-solucion::placeholder {
            color: #8e8e98;
            -webkit-text-fill-color: #8e8e98;
            opacity: 1;
        }

        .atencion-solucion:focus {
            border-color: #9783db;
        }

        .atencion-mensaje {
            padding: 14px 17px;
            margin: 0 0 20px;
            color: #c9baf4;
            background: #141018;
            border: 1px solid #40304d;
            border-radius: 7px;
            font-size: 12px;
            line-height: 1.6;
        }

        .atencion-mensaje.error {
            color: #e6a5ad;
            background: #1a1013;
            border-color: #4c2931;
        }

        .atencion-ayuda {
            margin-top: 14px;
            color: #9999a3;
            font-size: 11px;
            line-height: 1.7;
        }

        @media (max-width: 800px) {
            .atencion-metricas {
                grid-template-columns: 1fr;
            }
        }
    `;

    document.head.appendChild(estilos);

    seccion.innerHTML = `
        <div class="page-heading">
            <div>
                <h1>Atención de solicitudes</h1>
                <p>Cola FIFO y resolución de tickets de soporte TI.</p>
            </div>
        </div>

        <div id="atencionMensaje"
             class="atencion-mensaje"
             role="status"
             hidden></div>

        <div class="atencion-metricas">
            <article class="atencion-metrica">
                <span>Pendientes en cola</span>
                <strong id="atencionTotalPendientes">—</strong>
            </article>

            <article class="atencion-metrica">
                <span>Mis tickets asignados</span>
                <strong id="atencionTotalAsignados">—</strong>
            </article>

            <article class="atencion-metrica">
                <span>Espacios disponibles</span>
                <strong id="atencionDisponibles">—</strong>
            </article>
        </div>

        <section class="atencion-capacidad">
            <div class="atencion-capacidad-cabecera">
                <div>
                    <h2>Capacidad de atención</h2>
                    <p>Máximo de tres tickets activos por técnico.</p>
                </div>
                <strong id="atencionCapacidadValor"
                        class="atencion-capacidad-valor">— / 3</strong>
            </div>

            <div id="atencionCapacidadBarra"
                 class="atencion-capacidad-barra"
                 aria-label="Capacidad del técnico">
            </div>

            <p id="atencionCapacidadAviso"
               class="atencion-capacidad-aviso">
                Consultando capacidad...
            </p>
        </section>

        <section class="atencion-panel">
            <div class="atencion-panel-cabecera">
                <div>
                    <h2>Cola de atención FIFO</h2>
                    <p>Solicitudes disponibles por orden de llegada.</p>
                </div>

                <div class="atencion-botones">
                    <button id="atencionActualizar"
                            class="button button-secondary"
                            type="button">
                        Actualizar
                    </button>

                    <button id="atencionTomar"
                            class="button button-primary"
                            type="button"
                            disabled>
                        Tomar siguiente
                    </button>
                </div>
            </div>

            <div class="atencion-tabla-contenedor">
                <table class="atencion-tabla">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Título</th>
                            <th>Fecha de creación</th>
                            <th>Estado</th>
                        </tr>
                    </thead>
                    <tbody id="atencionPendientesBody"></tbody>
                </table>
            </div>
        </section>

        <section class="atencion-panel">
            <div class="atencion-panel-cabecera">
                <div>
                    <h2>Mis tickets asignados</h2>
                    <p>Selecciona un ticket para iniciar o resolver su atención.</p>
                </div>
            </div>

            <div class="atencion-tabla-contenedor">
                <table class="atencion-tabla">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Título</th>
                            <th>Fecha de creación</th>
                            <th>Estado</th>
                        </tr>
                    </thead>
                    <tbody id="atencionAsignadosBody"></tbody>
                </table>
            </div>

            <div class="atencion-detalle">
                <h3 id="atencionSeleccionado">
                    Ningún ticket seleccionado
                </h3>

                <p id="atencionIndicacion">
                    Selecciona uno de tus tickets asignados.
                </p>

                <textarea id="atencionSolucion"
                          class="atencion-solucion"
                          placeholder="Describe la solución aplicada al ticket..."
                          maxlength="10000"
                          disabled></textarea>

                <div class="atencion-botones">
                    <button id="atencionIniciar"
                            class="button button-secondary"
                            type="button"
                            disabled>
                        Iniciar atención
                    </button>

                    <button id="atencionResolver"
                            class="button button-primary"
                            type="button"
                            disabled>
                        Resolver ticket
                    </button>
                </div>

                <p class="atencion-ayuda">
                    Primero inicia la atención del ticket.
                    Después registra la solución para resolverlo.
                    Resolver un ticket libera un espacio de capacidad.
                </p>
            </div>
        </section>
    `;

    const pendientesBody = $("atencionPendientesBody");
    const asignadosBody = $("atencionAsignadosBody");

    const actualizarBtn = $("atencionActualizar");
    const tomarBtn = $("atencionTomar");
    const iniciarBtn = $("atencionIniciar");
    const resolverBtn = $("atencionResolver");

    const solucionCampo = $("atencionSolucion");
    const tituloSeleccionado = $("atencionSeleccionado");
    const indicacion = $("atencionIndicacion");

    const mensajeElemento = $("atencionMensaje");

    function mostrarMensaje(texto, error = false) {
        mensajeElemento.textContent = texto;
        mensajeElemento.classList.toggle("error", error);
        mensajeElemento.hidden = !texto;
    }

    function agregarCelda(fila, contenido) {
        const celda = document.createElement("td");
        celda.textContent =
            contenido == null ? "" : String(contenido);
        fila.appendChild(celda);
        return celda;
    }

    function mostrarVacio(cuerpo, texto) {
        cuerpo.replaceChildren();

        const fila = document.createElement("tr");
        const celda = agregarCelda(fila, texto);

        celda.colSpan = 4;
        celda.className = "atencion-vacio";
        cuerpo.appendChild(fila);
    }

    function obtenerSeleccionado() {
        return asignados.find(
            ticket => ticket.id === seleccionadoId
        ) || null;
    }

    function actualizarCapacidad() {

        $("atencionTotalPendientes").textContent =
            String(pendientes.length);

        $("atencionTotalAsignados").textContent =
            String(asignados.length);

        $("atencionDisponibles").textContent =
            capacidadDisponible
                ? String(Math.max(0, capacidadMaxima - ticketsActivos))
                : "—";

        $("atencionCapacidadValor").textContent =
            capacidadDisponible
                ? `${ticketsActivos} / ${capacidadMaxima}`
                : "— / 3";

        const barra = $("atencionCapacidadBarra");
        barra.replaceChildren();

        const totalSegmentos =
            capacidadDisponible ? capacidadMaxima : 3;

        barra.style.gridTemplateColumns =
            `repeat(${totalSegmentos}, minmax(0, 1fr))`;

        for (let i = 0; i < totalSegmentos; i++) {
            const segmento = document.createElement("div");

            segmento.className =
                "atencion-capacidad-segmento";

            if (capacidadDisponible && i < ticketsActivos) {
                segmento.classList.add("ocupado");

                if (ticketsActivos >= capacidadMaxima) {
                    segmento.classList.add("lleno");
                }
            }

            barra.appendChild(segmento);
        }

        const aviso = $("atencionCapacidadAviso");

        aviso.classList.remove("alerta");

        if (!capacidadDisponible) {
            aviso.textContent =
                "La capacidad todavía no está disponible.";
        } else if (ticketsActivos >= capacidadMaxima) {
            aviso.textContent =
                "Capacidad máxima alcanzada. "
                + "Resuelve uno de tus tickets para tomar otro.";
            aviso.classList.add("alerta");
        } else {
            const disponibles =
                capacidadMaxima - ticketsActivos;

            aviso.textContent =
                `Puedes tomar ${disponibles} `
                + (disponibles === 1
                    ? "ticket adicional."
                    : "tickets adicionales.");
        }
    }

    function actualizarBotones() {

        const ticket = obtenerSeleccionado();

        const estado = ticket
            ? String(ticket.estado).toUpperCase()
            : "";

        const ocupado = consultando || operando;

        actualizarBtn.disabled = ocupado;

        tomarBtn.disabled =
            ocupado
            || !capacidadDisponible
            || ticketsActivos >= capacidadMaxima
            || pendientes.length === 0;

        iniciarBtn.disabled =
            ocupado || estado !== "PENDIENTE";

        resolverBtn.disabled =
            ocupado || estado !== "EN_PROCESO";

        solucionCampo.disabled =
            ocupado || estado !== "EN_PROCESO";

        if (!ticket) {
            tituloSeleccionado.textContent =
                "Ningún ticket seleccionado";

            indicacion.textContent =
                "Selecciona uno de tus tickets asignados.";
        } else {
            tituloSeleccionado.textContent =
                `Ticket #${ticket.id} — ${ticket.titulo}`;

            indicacion.textContent =
                estado === "PENDIENTE"
                    ? "Este ticket está asignado. Inicia su atención."
                    : "Registra la solución para resolver este ticket.";
        }
    }

    function renderizarFilas(
        cuerpo,
        filas,
        seleccionables
    ) {

        cuerpo.replaceChildren();

        if (!filas.length) {
            mostrarVacio(
                cuerpo,
                seleccionables
                    ? "No tienes tickets asignados."
                    : "No hay tickets disponibles en la cola FIFO."
            );
            return;
        }

        filas.forEach(ticket => {

            const fila = document.createElement("tr");

            agregarCelda(fila, ticket.id);
            agregarCelda(fila, ticket.titulo);
            agregarCelda(fila, ticket.fecha);

            const celdaEstado = agregarCelda(fila, "");
            const estado = document.createElement("span");

            estado.className = "atencion-estado";
            estado.textContent = ticket.estado;

            if (String(ticket.estado).toUpperCase()
                === "EN_PROCESO") {
                estado.classList.add("proceso");
            }

            celdaEstado.appendChild(estado);

            if (seleccionables) {

                fila.classList.add("seleccionable");
                fila.tabIndex = 0;

                if (ticket.id === seleccionadoId) {
                    fila.classList.add("seleccionado");
                }

                const seleccionar = () => {

                    if (consultando || operando) {
                        return;
                    }

                    if (seleccionadoId !== ticket.id) {
                        solucionCampo.value = "";
                    }

                    seleccionadoId = ticket.id;

                    renderizarFilas(
                        asignadosBody,
                        asignados,
                        true
                    );

                    actualizarBotones();
                };

                fila.addEventListener("click", seleccionar);

                fila.addEventListener("keydown", evento => {
                    if (evento.key === "Enter"
                        || evento.key === " ") {

                        evento.preventDefault();
                        seleccionar();
                    }
                });
            }

            cuerpo.appendChild(fila);
        });
    }

    function renderizar() {

        if (!obtenerSeleccionado()) {
            seleccionadoId = null;
            solucionCampo.value = "";
        }

        actualizarCapacidad();

        renderizarFilas(
            pendientesBody,
            pendientes,
            false
        );

        renderizarFilas(
            asignadosBody,
            asignados,
            true
        );

        actualizarBotones();
    }

    function consultar() {

        if (consultando || operando) {
            return;
        }

        consultando = true;
        capacidadDisponible = false;

        actualizarCapacidad();
        actualizarBotones();

        try {
            puente.listarDatos();

        } catch (error) {

            consultando = false;

            actualizarBotones();

            mostrarMensaje(
                "No se pudo consultar la información de atención.",
                true
            );
        }
    }

    function ejecutar(
        accion,
        ticketId = 0,
        solucion = ""
    ) {

        if (operando || consultando) {
            return;
        }

        operando = true;
        capacidadDisponible = false;

        actualizarCapacidad();
        actualizarBotones();

        try {

            puente.ejecutarAccion(
                accion,
                ticketId,
                solucion
            );

        } catch (error) {

            operando = false;
            actualizarBotones();

            mostrarMensaje(
                "No se pudo comunicar con el servicio de atención.",
                true
            );
        }
    }

    window.nautaAtencionDataReady = datos => {

        consultando = false;

        pendientes = Array.isArray(datos.pendientes)
            ? datos.pendientes
            : [];

        asignados = Array.isArray(datos.asignados)
            ? datos.asignados
            : [];

        const activos = Number(datos.ticketsActivos);
        const maximo = Number(datos.capacidadMaxima);

        capacidadDisponible =
            Number.isInteger(activos)
            && Number.isInteger(maximo)
            && activos >= 0
            && maximo > 0;

        if (capacidadDisponible) {
            ticketsActivos = activos;
            capacidadMaxima = maximo;
        }

        renderizar();
        mostrarMensaje("");
    };

    window.nautaAtencionDataError = texto => {

        consultando = false;
        capacidadDisponible = false;

        actualizarCapacidad();
        actualizarBotones();

        mostrarMensaje(
            texto || "No se pudieron cargar los datos.",
            true
        );
    };

    window.nautaAtencionActionResult = resultado => {

        operando = false;

        if (!resultado.correcto) {

            actualizarBotones();

            mostrarMensaje(
                resultado.mensaje || "Operación no completada.",
                true
            );

            // Refrescar datos para conocer la capacidad real.
            consultar();
            return;
        }

        mostrarMensaje(
            resultado.mensaje || "Operación realizada."
        );

        // No descartamos la solución escrita hasta
        // que finaliza la operación correctamente.
        solucionCampo.value = "";

        consultar();

        if (window.nautaTicketBridge) {
            try {
                window.nautaTicketBridge.listarDatos();
            } catch (error) {
                console.warn(
                    "No se pudo actualizar el resumen de tickets.",
                    error
                );
            }
        }
    };

    actualizarBtn.addEventListener("click", consultar);

    tomarBtn.addEventListener("click", () => {

        if (!capacidadDisponible
            || ticketsActivos >= capacidadMaxima
            || pendientes.length === 0) {
            return;
        }

        ejecutar("tomar");
    });

    iniciarBtn.addEventListener("click", () => {

        const ticket = obtenerSeleccionado();

        if (!ticket
            || String(ticket.estado).toUpperCase()
            !== "PENDIENTE") {
            return;
        }

        ejecutar("iniciar", ticket.id);
    });

    resolverBtn.addEventListener("click", () => {

        const ticket = obtenerSeleccionado();

        if (!ticket
            || String(ticket.estado).toUpperCase()
            !== "EN_PROCESO") {
            return;
        }

        const solucion = solucionCampo.value.trim();

        if (!solucion) {
            mostrarMensaje(
                "Debes escribir la solución del ticket.",
                true
            );

            solucionCampo.focus();
            return;
        }

        ejecutar(
            "resolver",
            ticket.id,
            solucion
        );
    });

    document.querySelectorAll(
        '[data-section="atencion"]'
    ).forEach(boton => {

        boton.addEventListener("click", consultar);
    });

    renderizar();
    consultar();
})();
