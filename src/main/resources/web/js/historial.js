(function () {
    "use strict";

    const seccion = document.getElementById("historial");
    const puente = window.nautaHistorialBridge;

    if (!seccion || !puente) {
        return;
    }

    // JavaFX WebView no ofrece un selector de fecha nativo confiable.
    // Utilizamos el mismo tipo de calendario HTML que la sección Reportes.
    const meses = [
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    ];
    const diasSemana = ["Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do"];

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

        .historial-fecha-contenedor {
            position: relative;
            min-width: 0;
        }

        .historial-fecha-boton {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 10px;
            width: 100%;
            min-height: 42px;
            padding: 10px 12px;
            color: #f2f2f4;
            background: #080808;
            border: 1px solid #353535;
            border-radius: 6px;
            font: 12px "IBM Plex Sans", "Segoe UI", sans-serif;
            text-align: left;
            cursor: pointer;
        }

        .historial-fecha-boton:hover,
        .historial-fecha-boton:focus,
        .historial-fecha-boton[aria-expanded="true"] {
            border-color: #9783db;
            background: #111018;
            outline: none;
        }

        .historial-fecha-boton:disabled {
            opacity: .55;
            cursor: not-allowed;
        }

        .historial-fecha-boton svg {
            width: 17px;
            height: 17px;
            flex-shrink: 0;
            fill: none;
            stroke: #a99ad8;
            stroke-width: 1.8;
            stroke-linecap: round;
            stroke-linejoin: round;
        }

        .historial-fecha-vacia { color: #92929a; }

        .historial-calendario {
            position: absolute;
            top: calc(100% + 8px);
            left: 0;
            z-index: 150;
            width: 305px;
            max-width: 85vw;
            box-sizing: border-box;
            padding: 16px;
            background: #161616;
            color: #f2f2f4;
            border: 1px solid #45404f;
            border-radius: 9px;
            box-shadow: 0 14px 38px rgba(0,0,0,.65);
        }

        .historial-calendario[hidden] { display: none !important; }

        .historial-calendario-cabecera {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 10px;
            margin-bottom: 15px;
        }

        .historial-calendario-cabecera strong {
            font-size: 13px;
            font-weight: 600;
        }

        .historial-calendario-nav {
            width: 32px;
            height: 32px;
            color: #d7c9f3;
            background: #24202c;
            border: 1px solid #3a3246;
            border-radius: 6px;
            font-size: 20px;
            cursor: pointer;
        }

        .historial-calendario-nav:hover { background: #393046; }

        .historial-calendario-grid {
            display: grid;
            grid-template-columns: repeat(7, minmax(0, 1fr));
            gap: 3px;
        }

        .historial-dia-semana {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 32px;
            color: #9999a3;
            font-size: 10px;
        }

        .historial-dia {
            display: flex;
            align-items: center;
            justify-content: center;
            min-width: 0;
            height: 34px;
            color: #e3e3e8;
            background: transparent;
            border: 1px solid transparent;
            border-radius: 5px;
            font-size: 11px;
            cursor: pointer;
        }

        .historial-dia:hover {
            color: #fff;
            background: #30263f;
        }

        .historial-dia.hoy { border-color: #665681; }

        .historial-dia.seleccionado {
            color: #fff;
            background: #7965bb;
            font-weight: 600;
        }

        .historial-calendario-pie {
            display: flex;
            justify-content: space-between;
            gap: 8px;
            margin-top: 12px;
            padding-top: 13px;
            border-top: 1px solid #353535;
        }

        .historial-calendario-pie button {
            padding: 7px 11px;
            color: #d4c6f0;
            background: #221c2c;
            border: 1px solid #40334f;
            border-radius: 5px;
            font-size: 11px;
            cursor: pointer;
        }

        .historial-calendario-pie button:hover { background: #352844; }

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
                <label id="historialDesdeLabel">Desde</label>
                <div class="historial-fecha-contenedor">
                    <input type="hidden" id="historialDesde" value="">
                    <button id="historialDesdeBoton"
                            class="historial-fecha-boton"
                            type="button"
                            aria-haspopup="dialog"
                            aria-expanded="false"
                            aria-labelledby="historialDesdeLabel">
                        <span id="historialDesdeTexto"
                              class="historial-fecha-vacia">Seleccionar fecha</span>
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <rect x="3" y="5" width="18" height="16" rx="2"/>
                            <path d="M7 3v4M17 3v4M3 10h18"/>
                        </svg>
                    </button>
                    <div id="historialDesdeCalendario"
                         class="historial-calendario"
                         role="dialog"
                         aria-label="Seleccionar fecha inicial"
                         hidden></div>
                </div>
            </div>

            <div class="historial-campo">
                <label id="historialHastaLabel">Hasta</label>
                <div class="historial-fecha-contenedor">
                    <input type="hidden" id="historialHasta" value="">
                    <button id="historialHastaBoton"
                            class="historial-fecha-boton"
                            type="button"
                            aria-haspopup="dialog"
                            aria-expanded="false"
                            aria-labelledby="historialHastaLabel">
                        <span id="historialHastaTexto"
                              class="historial-fecha-vacia">Seleccionar fecha</span>
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <rect x="3" y="5" width="18" height="16" rx="2"/>
                            <path d="M7 3v4M17 3v4M3 10h18"/>
                        </svg>
                    </button>
                    <div id="historialHastaCalendario"
                         class="historial-calendario"
                         role="dialog"
                         aria-label="Seleccionar fecha final"
                         hidden></div>
                </div>
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

    const botonesFecha = {
        desde: $("historialDesdeBoton"),
        hasta: $("historialHastaBoton")
    };
    const textosFecha = {
        desde: $("historialDesdeTexto"),
        hasta: $("historialHastaTexto")
    };
    const calendarios = {
        desde: $("historialDesdeCalendario"),
        hasta: $("historialHastaCalendario")
    };

    const camposFecha = { desde: desde, hasta: hasta };
    let calendarioAbierto = null;
    let mesVisible = new Date();

    function rellenar(numero) {
        return String(numero).padStart(2, "0");
    }

    function fechaISO(anio, mes, dia) {
        return anio + "-" + rellenar(mes + 1) + "-" + rellenar(dia);
    }

    function fechaVisible(iso) {
        if (!iso) return "Seleccionar fecha";
        const partes = iso.split("-");
        return partes[2] + "/" + partes[1] + "/" + partes[0];
    }

    function actualizarCampoFecha(tipo) {
        const valor = camposFecha[tipo].value;
        textosFecha[tipo].textContent = fechaVisible(valor);
        textosFecha[tipo].classList.toggle("historial-fecha-vacia", !valor);
    }

    function cerrarCalendario() {
        calendarioAbierto = null;
        ["desde", "hasta"].forEach(tipo => {
            calendarios[tipo].hidden = true;
            botonesFecha[tipo].setAttribute("aria-expanded", "false");
        });
    }

    function asignarFecha(tipo, valor) {
        camposFecha[tipo].value = valor;
        actualizarCampoFecha(tipo);
        cerrarCalendario();
    }

    function abrirCalendario(tipo) {
        if (consultando) return;

        if (calendarioAbierto === tipo) {
            cerrarCalendario();
            return;
        }

        cerrarCalendario();
        calendarioAbierto = tipo;

        const valor = camposFecha[tipo].value;
        if (valor) {
            const partes = valor.split("-").map(Number);
            mesVisible = new Date(partes[0], partes[1] - 1, 1);
        } else {
            const hoy = new Date();
            mesVisible = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
        }

        calendarios[tipo].hidden = false;
        botonesFecha[tipo].setAttribute("aria-expanded", "true");
        dibujarCalendario(tipo);
    }

    function dibujarCalendario(tipo) {
        const contenedor = calendarios[tipo];
        contenedor.replaceChildren();

        const anio = mesVisible.getFullYear();
        const mes = mesVisible.getMonth();

        const cabecera = document.createElement("div");
        cabecera.className = "historial-calendario-cabecera";

        const anterior = document.createElement("button");
        anterior.type = "button";
        anterior.className = "historial-calendario-nav";
        anterior.textContent = "‹";
        anterior.setAttribute("aria-label", "Mes anterior");

        const titulo = document.createElement("strong");
        titulo.textContent = meses[mes] + " " + anio;

        const siguiente = document.createElement("button");
        siguiente.type = "button";
        siguiente.className = "historial-calendario-nav";
        siguiente.textContent = "›";
        siguiente.setAttribute("aria-label", "Mes siguiente");

        anterior.addEventListener("click", () => {
            mesVisible = new Date(anio, mes - 1, 1);
            dibujarCalendario(tipo);
        });
        siguiente.addEventListener("click", () => {
            mesVisible = new Date(anio, mes + 1, 1);
            dibujarCalendario(tipo);
        });

        cabecera.append(anterior, titulo, siguiente);
        contenedor.appendChild(cabecera);

        const cuadricula = document.createElement("div");
        cuadricula.className = "historial-calendario-grid";

        diasSemana.forEach(nombre => {
            const etiqueta = document.createElement("span");
            etiqueta.className = "historial-dia-semana";
            etiqueta.textContent = nombre;
            cuadricula.appendChild(etiqueta);
        });

        const inicio = (new Date(anio, mes, 1).getDay() + 6) % 7;
        for (let i = 0; i < inicio; i++) {
            cuadricula.appendChild(document.createElement("span"));
        }

        const hoy = new Date();
        const hoyISO = fechaISO(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
        const diasDelMes = new Date(anio, mes + 1, 0).getDate();

        for (let dia = 1; dia <= diasDelMes; dia++) {
            const valor = fechaISO(anio, mes, dia);
            const boton = document.createElement("button");
            boton.type = "button";
            boton.className = "historial-dia";
            boton.textContent = String(dia);
            boton.setAttribute("aria-label", fechaVisible(valor));
            boton.setAttribute("aria-pressed", String(valor === camposFecha[tipo].value));

            if (valor === hoyISO) boton.classList.add("hoy");
            if (valor === camposFecha[tipo].value) boton.classList.add("seleccionado");

            boton.addEventListener("click", () => asignarFecha(tipo, valor));
            cuadricula.appendChild(boton);
        }

        contenedor.appendChild(cuadricula);

        const pie = document.createElement("div");
        pie.className = "historial-calendario-pie";

        const hoyBtn = document.createElement("button");
        hoyBtn.type = "button";
        hoyBtn.textContent = "Hoy";
        hoyBtn.addEventListener("click", () => asignarFecha(tipo, hoyISO));

        const quitarBtn = document.createElement("button");
        quitarBtn.type = "button";
        quitarBtn.textContent = "Quitar fecha";
        quitarBtn.addEventListener("click", () => asignarFecha(tipo, ""));

        pie.append(hoyBtn, quitarBtn);
        contenedor.appendChild(pie);
    }

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
        cerrarCalendario();

        buscarBtn.disabled = true;
        limpiarBtn.disabled = true;
        botonesFecha.desde.disabled = true;
        botonesFecha.hasta.disabled = true;

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
            botonesFecha.desde.disabled = false;
            botonesFecha.hasta.disabled = false;

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
        botonesFecha.desde.disabled = false;
        botonesFecha.hasta.disabled = false;

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
        botonesFecha.desde.disabled = false;
        botonesFecha.hasta.disabled = false;

        contador.textContent = "Consulta no disponible";

        mensaje(
            texto || "No se pudo consultar el historial.",
            true
        );
    };

    botonesFecha.desde.addEventListener("click", () => abrirCalendario("desde"));
    botonesFecha.hasta.addEventListener("click", () => abrirCalendario("hasta"));

    document.addEventListener("click", evento => {
        if (!calendarioAbierto) return;

        const boton = botonesFecha[calendarioAbierto];
        const calendario = calendarios[calendarioAbierto];
        if (!boton.contains(evento.target) &&
            !calendario.contains(evento.target)) {
            cerrarCalendario();
        }
    });

    document.addEventListener("keydown", evento => {
        if (evento.key === "Escape") cerrarCalendario();
    });

    formulario.addEventListener("submit", evento => {
        evento.preventDefault();
        consultar();
    });

    limpiarBtn.addEventListener("click", () => {
        ticketId.value = "";
        desde.value = "";
        hasta.value = "";
        actualizarCampoFecha("desde");
        actualizarCampoFecha("hasta");
        cerrarCalendario();
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
