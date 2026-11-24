
(function () {
    "use strict";

    const seccion = document.getElementById("reportes");
    const puente = window.nautaReporteBridge;

    if (!seccion || !puente) {
        return;
    }

    const meses = [
        "Enero", "Febrero", "Marzo", "Abril",
        "Mayo", "Junio", "Julio", "Agosto",
        "Septiembre", "Octubre", "Noviembre", "Diciembre"
    ];

    const diasSemana = [
        "Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do"
    ];

    let procesando = false;
    let calendarioAbierto = null;
    let mesActual = new Date();
    let fechaDesde = "";
    let fechaHasta = "";

    const estilos = document.createElement("style");

    estilos.textContent = `
        .reporte-formulario {
            padding: 24px;
            background: #0d0d0d;
            border: 1px solid #292929;
            border-radius: 9px;
        }

        .reporte-tipos {
            margin: 0 0 24px;
            padding: 0;
            border: 0;
        }

        .reporte-tipos legend {
            margin-bottom: 6px;
            color: #f2f2f4;
            font-size: 16px;
            font-weight: 600;
        }

        .reporte-tipos-ayuda {
            margin: 0 0 16px;
            color: #a6a6b0;
            font-size: 12px;
        }

        .reporte-tipos-grid {
            display: grid;
            grid-template-columns: repeat(4, minmax(0, 1fr));
            gap: 12px;
        }

        .reporte-tipo {
            display: flex;
            align-items: flex-start;
            gap: 10px;
            min-height: 77px;
            padding: 14px;
            box-sizing: border-box;
            background: #111111;
            border: 1px solid #333139;
            border-radius: 8px;
            cursor: pointer;
        }

        .reporte-tipo.seleccionado {
            background: #1e1828;
            border-color: #9783db;
        }

        .reporte-tipo input {
            margin: 3px 0 0;
            accent-color: #9783db;
        }

        .reporte-tipo strong, .reporte-tipo small {
            display: block;
        }

        .reporte-tipo strong {
            color: #f2f2f4;
            font-size: 13px;
        }

        .reporte-tipo small {
            margin-top: 5px;
            color: #a6a6b0;
            font-size: 11px;
            line-height: 1.4;
        }

        .reporte-disponibilidad {
            margin: 16px 0 0;
            padding: 12px 14px;
            color: #c9baf4;
            background: #17121e;
            border: 1px solid #40304d;
            border-radius: 6px;
            font-size: 12px;
        }

        .reporte-resumen {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 12px;
        }

        .reporte-resumen-item {
            padding: 15px;
            background: #141414;
            border: 1px solid #303030;
            border-radius: 7px;
        }

        .reporte-resumen-item strong {
            display: block;
            margin-bottom: 6px;
            color: #eee9fb;
            font-size: 13px;
        }

        .reporte-informacion .reporte-resumen-item p {
            margin: 0;
        }

        .reporte-grid {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 18px;
        }

        .reporte-campo {
            display: flex;
            flex-direction: column;
            gap: 9px;
            min-width: 0;
        }

        .reporte-campo label {
            color: #c7c7d0;
            font-size: 12px;
            font-weight: 500;
        }

        .reporte-input {
            width: 100%;
            min-height: 43px;
            box-sizing: border-box;
            padding: 10px 12px;
            color: #f2f2f4 !important;
            -webkit-text-fill-color: #f2f2f4;
            background-color: #080808 !important;
            border: 1px solid #353535;
            border-radius: 6px;
            outline: none;
            color-scheme: dark;
            font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
            font-size: 12px;
        }

        .reporte-input:focus {
            border-color: #9783db;
        }

        .reporte-input option {
            background: #111111;
            color: #f2f2f4;
        }

        .reporte-fecha-contenedor {
            position: relative;
        }

        .reporte-fecha-boton {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            width: 100%;
            min-height: 43px;
            padding: 10px 12px;
            color: #f2f2f4;
            background: #080808;
            border: 1px solid #353535;
            border-radius: 6px;
            font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
            font-size: 12px;
            text-align: left;
            cursor: pointer;
        }

        .reporte-fecha-boton:hover,
        .reporte-fecha-boton[aria-expanded="true"] {
            border-color: #9783db;
            background: #101010;
        }

        .reporte-fecha-boton:disabled {
            opacity: .5;
            cursor: not-allowed;
        }

        .reporte-fecha-boton svg {
            width: 17px;
            height: 17px;
            flex-shrink: 0;
            fill: none;
            stroke: #a99ad8;
            stroke-width: 1.8;
            stroke-linecap: round;
            stroke-linejoin: round;
        }

        .reporte-fecha-vacia {
            color: #92929a;
        }

        .reporte-calendario {
            position: absolute;
            left: 0;
            top: calc(100% + 8px);
            z-index: 150;
            width: 305px;
            max-width: 85vw;
            padding: 16px;
            color: #f2f2f4;
            background: #161616;
            border: 1px solid #45404f;
            border-radius: 9px;
            box-shadow: 0 14px 38px rgba(0,0,0,.65);
        }

        .reporte-calendario[hidden] {
            display: none !important;
        }

        .reporte-calendario-cabecera {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 10px;
            margin-bottom: 15px;
        }

        .reporte-calendario-cabecera strong {
            font-size: 13px;
            font-weight: 600;
        }

        .reporte-calendario-nav {
            width: 32px;
            height: 32px;
            color: #d7c9f3;
            background: #24202c;
            border: 1px solid #3a3246;
            border-radius: 6px;
            font-size: 20px;
            cursor: pointer;
        }

        .reporte-calendario-nav:hover {
            background: #393046;
        }

        .reporte-calendario-grid {
            display: grid;
            grid-template-columns: repeat(7, minmax(0, 1fr));
            gap: 3px;
        }

        .reporte-dia-semana {
            display: flex;
            justify-content: center;
            align-items: center;
            height: 32px;
            color: #9999a3;
            font-size: 10px;
        }

        .reporte-dia {
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

        .reporte-dia:hover {
            color: #ffffff;
            background: #30263f;
        }

        .reporte-dia.hoy {
            border-color: #665681;
        }

        .reporte-dia.seleccionado {
            color: #ffffff;
            background: #7965bb;
            font-weight: 600;
        }

        .reporte-calendario-pie {
            display: flex;
            justify-content: space-between;
            gap: 8px;
            padding-top: 13px;
            margin-top: 12px;
            border-top: 1px solid #353535;
        }

        .reporte-calendario-pie button {
            padding: 7px 11px;
            color: #d4c6f0;
            background: #221c2c;
            border: 1px solid #40334f;
            border-radius: 5px;
            font-size: 11px;
            cursor: pointer;
        }

        .reporte-calendario-pie button:hover {
            background: #352844;
        }

        .reporte-ayuda {
            margin: 17px 0 0;
            color: #9999a3;
            font-size: 12px;
            line-height: 1.7;
        }

        .reporte-acciones {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            margin-top: 24px;
            padding-top: 21px;
            border-top: 1px solid #292929;
        }

        .reporte-mensaje {
            margin-top: 20px;
            padding: 15px 17px;
            background: #141018;
            border: 1px solid #40304d;
            border-radius: 7px;
            color: #c9baf4;
            font-size: 12px;
            line-height: 1.7;
            overflow-wrap: anywhere;
        }

        .reporte-mensaje.error {
            color: #e6a5ad;
            background: #1a1013;
            border-color: #4c2931;
        }

        .reporte-mensaje strong {
            display: block;
            margin-bottom: 6px;
            font-size: 13px;
        }

        .reporte-ruta {
            display: block;
            margin-top: 7px;
            color: #b1b1bd;
            font-family: "IBM Plex Mono", Consolas, monospace;
            font-size: 11px;
            overflow-wrap: anywhere;
        }

        .reporte-informacion {
            margin-top: 23px;
            padding: 23px;
            background: #0d0d0d;
            border: 1px solid #292929;
            border-radius: 9px;
        }

        .reporte-informacion h2 {
            margin: 0 0 12px;
            color: #f2f2f4;
            font-size: 15px;
            font-weight: 600;
        }

        .reporte-informacion p {
            margin: 0 0 12px;
            color: #a6a6b0;
            font-size: 12px;
            line-height: 1.8;
        }

        .reporte-informacion p:last-child {
            margin-bottom: 0;
        }

        .reporte-vista-previa {
            margin-top: 18px;
            border: 1px solid #40304d;
            border-radius: 7px;
            overflow: hidden;
            background: #ffffff;
        }

        .reporte-vista-cabecera {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            padding: 12px 14px;
            color: #e9e2fa;
            background: #17121e;
            border-bottom: 1px solid #40304d;
            font-size: 12px;
        }

        .reporte-vista-cabecera button {
            flex: 0 0 auto;
        }

        .reporte-vista-documento {
            display: block;
            width: 100%;
            height: 640px;
            border: 0;
            background: #ffffff;
        }

        @media (max-width: 850px) {
            .reporte-grid, .reporte-tipos-grid, .reporte-resumen {
                grid-template-columns: 1fr;
            }

            .reporte-calendario {
                width: 290px;
            }
        }
    `;

    document.head.appendChild(estilos);

    seccion.innerHTML = `
        <div class="page-heading">
            <div>
                <h1>Reportes</h1>
                <p>
                    Genera documentos PDF con los tickets
                    registrados en NautaTicket.
                </p>
            </div>
        </div>

        <form id="reporteFormulario"
              class="reporte-formulario">

            <fieldset class="reporte-tipos">
                <legend>Tipo de informe</legend>
                <p class="reporte-tipos-ayuda">Elige qué contenido quieres consultar en la vista previa y guardar en PDF.</p>
                <div class="reporte-tipos-grid">
                    <label class="reporte-tipo">
                        <input type="radio" name="tipoReporte" value="TICKETS">
                        <span><strong>Tickets</strong><small>Listado según los filtros seleccionados.</small></span>
                    </label>
                    <label class="reporte-tipo">
                        <input type="radio" name="tipoReporte" value="HISTORIAL">
                        <span><strong>Historial</strong><small>Movimientos y cambios de los tickets.</small></span>
                    </label>
                    <label class="reporte-tipo">
                        <input type="radio" name="tipoReporte" value="GRAFICOS">
                        <span><strong>Gráficos</strong><small>Indicadores por estado y categoría.</small></span>
                    </label>
                    <label class="reporte-tipo">
                        <input type="radio" name="tipoReporte" value="GENERAL" checked>
                        <span><strong>General</strong><small>Vista conjunta disponible actualmente.</small></span>
                    </label>
                </div>
                <p id="reporteDisponibilidad" class="reporte-disponibilidad" role="status" hidden></p>
            </fieldset>

            <div class="reporte-grid">

                <div class="reporte-campo">
                    <label id="reporteDesdeLabel">
                        Fecha inicial
                    </label>

                    <div class="reporte-fecha-contenedor">
                        <button id="reporteDesdeBoton"
                                type="button"
                                class="reporte-fecha-boton"
                                aria-labelledby="reporteDesdeLabel"
                                aria-expanded="false">
                            <span id="reporteDesdeTexto"
                                  class="reporte-fecha-vacia">
                                Seleccionar fecha
                            </span>
                            <svg viewBox="0 0 24 24">
                                <rect x="3" y="5"
                                      width="18" height="16"
                                      rx="2"/>
                                <path d="M7 3v4M17 3v4M3 10h18"/>
                            </svg>
                        </button>

                        <div id="reporteDesdeCalendario"
                             class="reporte-calendario"
                             hidden></div>
                    </div>
                </div>

                <div class="reporte-campo">
                    <label id="reporteHastaLabel">
                        Fecha final
                    </label>

                    <div class="reporte-fecha-contenedor">
                        <button id="reporteHastaBoton"
                                type="button"
                                class="reporte-fecha-boton"
                                aria-labelledby="reporteHastaLabel"
                                aria-expanded="false">
                            <span id="reporteHastaTexto"
                                  class="reporte-fecha-vacia">
                                Seleccionar fecha
                            </span>
                            <svg viewBox="0 0 24 24">
                                <rect x="3" y="5"
                                      width="18" height="16"
                                      rx="2"/>
                                <path d="M7 3v4M17 3v4M3 10h18"/>
                            </svg>
                        </button>

                        <div id="reporteHastaCalendario"
                             class="reporte-calendario"
                             hidden></div>
                    </div>
                </div>

                <div class="reporte-campo">
                    <label for="reporteEstado">
                        Estado del ticket
                    </label>

                    <select id="reporteEstado"
                            class="reporte-input">
                        <option value="TODOS">
                            Todos los estados
                        </option>
                        <option value="PENDIENTE">
                            Pendiente
                        </option>
                        <option value="EN_PROCESO">
                            En proceso
                        </option>
                        <option value="RESUELTO">
                            Resuelto
                        </option>
                        <option value="CERRADO">
                            Cerrado
                        </option>
                    </select>
                </div>
            </div>

            <p class="reporte-ayuda">
                Selecciona las fechas desde el calendario.
                Si dejas las fechas vacías, se incluirá
                todo el período disponible.
                El formato visible es día/mes/año.
            </p>

            <div class="reporte-acciones">
                <button id="reporteGenerar"
                        class="button button-primary"
                        type="submit">
                    Generar vista previa
                </button>

                <button id="reporteLimpiar"
                        class="button button-secondary"
                        type="button">
                    Limpiar filtros
                </button>
            </div>
        </form>

        <div id="reporteMensaje"
             class="reporte-mensaje"
             role="status"
             hidden></div>

        <div class="reporte-informacion">
            <h2 id="reporteContenidoTitulo">Informe general</h2>
            <div id="reporteContenidoResumen" class="reporte-resumen"></div>

            <div id="reporteVistaPrevia"
                 class="reporte-vista-previa"
                 hidden>
                <div class="reporte-vista-cabecera">
                    <span id="reporteVistaTitulo">Vista previa del informe general</span>
                    <button id="reporteGuardar"
                            class="button button-primary"
                            type="button">
                        Guardar PDF
                    </button>
                </div>
                <iframe id="reporteVistaDocumento"
                        class="reporte-vista-documento"
                        title="Vista previa del reporte"
                        sandbox></iframe>
            </div>
        </div>
    `;

    const $ = id => document.getElementById(id);

    const formulario = $("reporteFormulario");
    const estado = $("reporteEstado");
    const tipos = Array.from(formulario.querySelectorAll('input[name="tipoReporte"]'));
    const disponibilidad = $("reporteDisponibilidad");
    const contenidoTitulo = $("reporteContenidoTitulo");
    const contenidoResumen = $("reporteContenidoResumen");
    const vistaTitulo = $("reporteVistaTitulo");

    const generarBtn = $("reporteGenerar");
    const limpiarBtn = $("reporteLimpiar");
    const mensajeElemento = $("reporteMensaje");
    const vistaPrevia = $("reporteVistaPrevia");
    const vistaDocumento = $("reporteVistaDocumento");
    const guardarBtn = $("reporteGuardar");

    const nombresTipo = {
        TICKETS: "Tickets",
        HISTORIAL: "Historial",
        GRAFICOS: "Gráficos",
        GENERAL: "General"
    };

    function tipoSeleccionado() {
        return tipos.find(radio => radio.checked).value;
    }

    function invalidarVista() {
        vistaPrevia.hidden = true;
        vistaDocumento.srcdoc = "";
        guardarBtn.disabled = true;
    }

    function actualizarTipo() {
        const tipo = tipoSeleccionado();
        tipos.forEach(radio => {
            radio.parentElement.classList.toggle("seleccionado", radio.checked);
        });
        generarBtn.disabled = procesando;
        disponibilidad.hidden = tipo !== "HISTORIAL";
        disponibilidad.textContent = tipo === "HISTORIAL"
            ? "El informe de historial está disponible para administradores."
            : "";
        contenidoTitulo.textContent = "Informe " + nombresTipo[tipo].toLowerCase();
        const detalles = tipo === "GENERAL"
            ? [
                ["Tickets", "Listado de tickets que cumplen las fechas y el estado elegidos, según tus permisos."],
                ["Gráficos", "Si eres administrador, se agregan al final el resumen por estado y los tiempos de resolución de los técnicos."],
                ["Vista previa y PDF", "Revisa el documento antes de guardarlo. El PDF conserva el mismo contenido de la vista previa."],
                ["Filtros", "Las fechas y el estado afectan al listado de tickets. Los gráficos del administrador usan datos globales."]
              ]
            : tipo === "TICKETS"
                ? [["Tickets", "Listado de tickets que cumplen las fechas y el estado elegidos, según tus permisos."],
                   ["Vista previa y PDF", "La vista previa muestra el mismo listado que se guardará en PDF."]]
                : tipo === "HISTORIAL"
                    ? [["Movimientos", "Cronología de cambios de estado y de técnico registrados en los tickets."],
                       ["Filtros y acceso", "Las fechas y el estado se aplican al historial. Este informe requiere permisos de administrador."]]
                    : [["Estadísticas", "Resumen de los tickets visibles para tu usuario: estados, tasa de resolución y categorías más frecuentes."],
                       ["Filtros", "Las fechas y el estado se aplican a los indicadores de esta plantilla."]];
        contenidoResumen.replaceChildren();
        detalles.forEach(([titulo, descripcion]) => {
            const item = document.createElement("div");
            item.className = "reporte-resumen-item";
            const encabezado = document.createElement("strong");
            encabezado.textContent = titulo;
            const texto = document.createElement("p");
            texto.textContent = descripcion;
            item.append(encabezado, texto);
            contenidoResumen.appendChild(item);
        });
        invalidarVista();
        mensajeElemento.hidden = true;
    }

    const botonesFecha = {
        desde: $("reporteDesdeBoton"),
        hasta: $("reporteHastaBoton")
    };

    const textosFecha = {
        desde: $("reporteDesdeTexto"),
        hasta: $("reporteHastaTexto")
    };

    const calendarios = {
        desde: $("reporteDesdeCalendario"),
        hasta: $("reporteHastaCalendario")
    };

    function rellenar(numero) {
        return String(numero).padStart(2, "0");
    }

    function fechaISO(anio, mes, dia) {
        return anio + "-" +
            rellenar(mes + 1) + "-" +
            rellenar(dia);
    }

    function mostrarFecha(iso) {
        if (!iso) return "Seleccionar fecha";

        const partes = iso.split("-");

        return partes[2] + "/" +
            partes[1] + "/" + partes[0];
    }

    function fechaSeleccionada(tipo) {
        return tipo === "desde" ? fechaDesde : fechaHasta;
    }

    function actualizarCampo(tipo) {
        const valor = fechaSeleccionada(tipo);
        const texto = textosFecha[tipo];

        texto.textContent = mostrarFecha(valor);
        texto.classList.toggle(
            "reporte-fecha-vacia",
            !valor
        );
    }

    function asignarFecha(tipo, valor) {
        if (tipo === "desde") {
            fechaDesde = valor;
        } else {
            fechaHasta = valor;
        }

        actualizarCampo(tipo);
        invalidarVista();
        cerrarCalendario();
    }

    function cerrarCalendario() {
        calendarioAbierto = null;

        ["desde", "hasta"].forEach(tipo => {
            calendarios[tipo].hidden = true;
            botonesFecha[tipo].setAttribute(
                "aria-expanded",
                "false"
            );
        });
    }

    function abrirCalendario(tipo) {
        if (procesando) return;

        if (calendarioAbierto === tipo) {
            cerrarCalendario();
            return;
        }

        cerrarCalendario();

        calendarioAbierto = tipo;

        const valor = fechaSeleccionada(tipo);

        if (valor) {
            const partes = valor.split("-").map(Number);
            mesActual = new Date(partes[0], partes[1] - 1, 1);
        } else {
            const hoy = new Date();
            mesActual = new Date(
                hoy.getFullYear(),
                hoy.getMonth(),
                1
            );
        }

        calendarios[tipo].hidden = false;
        botonesFecha[tipo].setAttribute(
            "aria-expanded",
            "true"
        );

        dibujarCalendario(tipo);
    }

    function dibujarCalendario(tipo) {
        const contenedor = calendarios[tipo];
        contenedor.replaceChildren();

        const anio = mesActual.getFullYear();
        const mes = mesActual.getMonth();

        const cabecera = document.createElement("div");
        cabecera.className = "reporte-calendario-cabecera";

        const anterior = document.createElement("button");
        anterior.type = "button";
        anterior.className = "reporte-calendario-nav";
        anterior.textContent = "‹";
        anterior.setAttribute("aria-label", "Mes anterior");

        const titulo = document.createElement("strong");
        titulo.textContent = meses[mes] + " " + anio;

        const siguiente = document.createElement("button");
        siguiente.type = "button";
        siguiente.className = "reporte-calendario-nav";
        siguiente.textContent = "›";
        siguiente.setAttribute("aria-label", "Mes siguiente");

        anterior.addEventListener("click", () => {
            mesActual = new Date(anio, mes - 1, 1);
            dibujarCalendario(tipo);
        });

        siguiente.addEventListener("click", () => {
            mesActual = new Date(anio, mes + 1, 1);
            dibujarCalendario(tipo);
        });

        cabecera.append(anterior, titulo, siguiente);
        contenedor.appendChild(cabecera);

        const cuadrícula = document.createElement("div");
        cuadrícula.className = "reporte-calendario-grid";

        diasSemana.forEach(nombre => {
            const diaSemana = document.createElement("span");
            diaSemana.className = "reporte-dia-semana";
            diaSemana.textContent = nombre;
            cuadrícula.appendChild(diaSemana);
        });

        // JavaScript usa domingo=0, pero nuestro calendario
        // comienza en lunes.
        const inicio =
            (new Date(anio, mes, 1).getDay() + 6) % 7;

        const cantidadDias =
            new Date(anio, mes + 1, 0).getDate();

        for (let i = 0; i < inicio; i++) {
            cuadrícula.appendChild(
                document.createElement("span")
            );
        }

        const hoy = new Date();
        const hoyISO = fechaISO(
            hoy.getFullYear(),
            hoy.getMonth(),
            hoy.getDate()
        );

        for (let dia = 1; dia <= cantidadDias; dia++) {
            const valor = fechaISO(anio, mes, dia);

            const boton = document.createElement("button");
            boton.type = "button";
            boton.className = "reporte-dia";
            boton.textContent = String(dia);
            boton.setAttribute(
                "aria-label",
                mostrarFecha(valor)
            );

            if (valor === hoyISO) {
                boton.classList.add("hoy");
            }

            if (valor === fechaSeleccionada(tipo)) {
                boton.classList.add("seleccionado");
            }

            boton.addEventListener("click", () => {
                asignarFecha(tipo, valor);
            });

            cuadrícula.appendChild(boton);
        }

        contenedor.appendChild(cuadrícula);

        const pie = document.createElement("div");
        pie.className = "reporte-calendario-pie";

        const hoyBtn = document.createElement("button");
        hoyBtn.type = "button";
        hoyBtn.textContent = "Hoy";

        hoyBtn.addEventListener("click", () => {
            asignarFecha(tipo, hoyISO);
        });

        const borrarBtn = document.createElement("button");
        borrarBtn.type = "button";
        borrarBtn.textContent = "Quitar fecha";

        borrarBtn.addEventListener("click", () => {
            asignarFecha(tipo, "");
        });

        pie.append(hoyBtn, borrarBtn);
        contenedor.appendChild(pie);
    }

    function mostrarMensaje(
        texto,
        error = false,
        archivo = ""
    ) {
        mensajeElemento.replaceChildren();
        mensajeElemento.hidden = false;
        mensajeElemento.classList.toggle("error", error);

        const titulo = document.createElement("strong");
        titulo.textContent = error
            ? "No se generó el reporte"
            : "Reporte generado";

        const contenido = document.createElement("span");
        contenido.textContent = texto;

        mensajeElemento.append(titulo, contenido);

        if (archivo) {
            const ruta = document.createElement("span");
            ruta.className = "reporte-ruta";
            ruta.textContent =
                "Archivo guardado en: " + archivo;
            mensajeElemento.appendChild(ruta);
        }
    }

    function bloquearFormulario(bloqueado) {
        procesando = bloqueado;

        generarBtn.disabled = bloqueado;
        limpiarBtn.disabled = bloqueado;
        estado.disabled = bloqueado;
        tipos.forEach(radio => { radio.disabled = bloqueado; });

        botonesFecha.desde.disabled = bloqueado;
        botonesFecha.hasta.disabled = bloqueado;

        generarBtn.textContent = bloqueado
            ? "Generando reporte..."
            : "Generar vista previa";

        if (bloqueado) {
            cerrarCalendario();
        }
    }

    function generarReporte() {
        if (procesando) return;
        const tipo = tipoSeleccionado();

        if (fechaDesde && fechaHasta &&
            fechaDesde > fechaHasta) {
            mostrarMensaje(
                "La fecha inicial no puede ser posterior a la final.",
                true
            );
            return;
        }

        bloquearFormulario(true);
        invalidarVista();
        mensajeElemento.hidden = true;

        try {
            puente.generarReporte(
                fechaDesde,
                fechaHasta,
                estado.value,
                tipo
            );
        } catch (error) {
            bloquearFormulario(false);

            mostrarMensaje(
                "No se pudo iniciar la generación del PDF.",
                true
            );
        }
    }

    window.nautaReporteResultado = resultado => {
        bloquearFormulario(false);

        if (!resultado) {
            mostrarMensaje(
                "No se recibió una respuesta válida.",
                true
            );
            return;
        }

        if (resultado.correcto && resultado.vistaPrevia) {
            vistaDocumento.srcdoc = resultado.vistaPrevia;
            vistaPrevia.hidden = false;
            guardarBtn.disabled = false;
            vistaTitulo.textContent = "Vista previa del informe " +
                nombresTipo[tipoSeleccionado()].toLowerCase();
        } else if (!vistaPrevia.hidden) {
            guardarBtn.disabled = false;
        }

        mostrarMensaje(
            resultado.mensaje ||
            "No se recibió información del reporte.",
            !resultado.correcto,
            resultado.correcto ? resultado.archivo : ""
        );
    };

    guardarBtn.addEventListener("click", () => {
        if (procesando) return;

        guardarBtn.disabled = true;
        mostrarMensaje("Selecciona la ubicación donde guardar el PDF.");

        try {
            puente.guardarReporte();
        } catch (error) {
            guardarBtn.disabled = false;
            mostrarMensaje("No se pudo iniciar el guardado del PDF.", true);
        }
    });

    botonesFecha.desde.addEventListener("click", () => {
        abrirCalendario("desde");
    });

    botonesFecha.hasta.addEventListener("click", () => {
        abrirCalendario("hasta");
    });

    tipos.forEach(radio => radio.addEventListener("change", actualizarTipo));
    estado.addEventListener("change", invalidarVista);

    // Cerrar al hacer clic fuera del calendario.
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
        if (evento.key === "Escape") {
            cerrarCalendario();
        }
    });

    formulario.addEventListener("submit", evento => {
        evento.preventDefault();
        generarReporte();
    });

    limpiarBtn.addEventListener("click", () => {
        if (procesando) return;

        fechaDesde = "";
        fechaHasta = "";
        estado.value = "TODOS";
        formulario.querySelector('input[value="GENERAL"]').checked = true;
        actualizarTipo();

        actualizarCampo("desde");
        actualizarCampo("hasta");
        cerrarCalendario();

        mensajeElemento.hidden = true;
        invalidarVista();
    });

    actualizarCampo("desde");
    actualizarCampo("hasta");
    actualizarTipo();
})();
