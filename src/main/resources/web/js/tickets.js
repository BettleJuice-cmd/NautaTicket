(function () {
    "use strict";

    const $ = id => document.getElementById(id);

    const puente = window.nautaTicketBridge;

    const seccion = $("tickets");

    if (!seccion || !puente) {
        return;
    }

    let tickets = [];
    let categorias = [];

    let consultando = false;
    let operando = false;

    let modal = null;

    const estilos = document.createElement("style");

    estilos.textContent = `
        .tickets-toolbar {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
            margin-bottom: 20px;
        }

        .tickets-input,
        .tickets-select,
        .tickets-textarea {
            box-sizing: border-box;
            color-scheme: dark;
            color: #f2f2f4 !important;
            -webkit-text-fill-color: #f2f2f4;
            background: #080808 !important;
            border: 1px solid #333333;
            border-radius: 6px;
            outline: none;
            font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
            font-size: 12px;
        }

        .tickets-input {
            flex: 1 1 230px;
            min-width: 160px;
            height: 42px;
            padding: 0 13px;
        }

        .tickets-select {
            min-width: 145px;
            min-height: 42px;
            padding: 0 12px;
        }

        .tickets-input::placeholder,
        .tickets-textarea::placeholder {
            color: #92929a !important;
            -webkit-text-fill-color: #92929a;
            opacity: 1;
        }

        .tickets-input:focus,
        .tickets-select:focus,
        .tickets-textarea:focus {
            border-color: #9783db;
            background: #0a0a0a !important;
        }

        .tickets-select option {
            color: #f2f2f4;
            background: #111111;
        }

        .tickets-feedback {
            margin: 0 0 18px;
            padding: 13px 16px;
            color: #c9baf4;
            background: #141018;
            border: 1px solid #3c304d;
            border-radius: 7px;
            font-size: 12px;
        }

        .tickets-feedback.error {
            color: #e7a1aa;
            background: #1a1013;
            border-color: #4a2830;
        }

        .tickets-count {
            margin: 12px 0;
            color: #9b9ba3;
            font-size: 12px;
        }

        .tickets-action {
            padding: 7px 11px;
            margin: 2px 6px 2px 0;
            color: #d3c5f5;
            background: #1a1522;
            border: 1px solid #3a3048;
            border-radius: 5px;
            font-size: 11px;
            cursor: pointer;
        }

        .tickets-action:hover {
            background: #292039;
        }

        .tickets-action.danger {
            color: #e4a2ac;
            background: #1c1215;
            border-color: #513038;
        }

        .tickets-status {
            display: inline-block;
            padding: 5px 9px;
            border-radius: 5px;
            border: 1px solid #363636;
            background: #171717;
            color: #ccccd2;
            font-size: 11px;
            white-space: nowrap;
        }

        .tickets-status.pending {
            color: #dfbc88;
            border-color: #51402c;
            background: #201910;
        }

        .tickets-status.progress {
            color: #9db8ec;
            border-color: #34435c;
            background: #111925;
        }

        .tickets-status.resolved {
            color: #a2d4b8;
            border-color: #30483a;
            background: #111c15;
        }

        .tickets-modal-overlay {
            position: fixed;
            inset: 0;
            z-index: 6000;
            display: flex;
            justify-content: center;
            align-items: center;
            background: rgba(0, 0, 0, .85);
            padding: 20px;
        }

        .tickets-modal-overlay[hidden] {
            display: none !important;
        }

        .tickets-modal {
            width: 100%;
            max-width: 560px;
            max-height: 92vh;
            overflow-y: auto;
            padding: 26px;
            color: #f2f2f4;
            background: #101010;
            border: 1px solid #343434;
            border-radius: 10px;
            font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
        }

        .tickets-modal h2 {
            margin: 0 0 10px;
            font-size: 19px;
            font-weight: 600;
        }

        .tickets-modal p {
            margin: 0 0 22px;
            color: #a4a4ab;
            font-size: 12px;
            line-height: 1.7;
        }

        .tickets-form {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .tickets-form label {
            display: flex;
            flex-direction: column;
            gap: 8px;
            color: #d2d2d7;
            font-size: 12px;
        }

        .tickets-form .tickets-input,
        .tickets-form .tickets-select {
            flex: none;
            width: 100%;
        }

        .tickets-textarea {
            width: 100%;
            min-height: 125px;
            padding: 12px;
            resize: vertical;
        }

        .tickets-modal-actions {
            display: flex;
            justify-content: flex-end;
            flex-wrap: wrap;
            gap: 10px;
            margin-top: 8px;
        }

        .tickets-modal-error {
            min-height: 17px;
            color: #e7a1aa;
            font-size: 12px;
        }

        .tickets-empty {
            text-align: center;
            color: #9b9ba3;
            padding: 38px 20px !important;
        }

        .tickets-caption {
            margin-top: 12px;
            color: #85858e;
            font-size: 11px;
            line-height: 1.6;
        }
    `;

    document.head.appendChild(estilos);

    // Se construye solamente la sección Tickets.
    seccion.innerHTML = `
        <div class="page-heading">
            <div>
                <h1>Gestión de tickets</h1>
                <p>Registro, consulta y seguimiento de solicitudes.</p>
            </div>
            <button id="ticketNuevoBtn"
                    type="button"
                    class="button button-primary">
                Nuevo ticket
            </button>
        </div>

        <div id="ticketsFeedback"
             class="tickets-feedback"
             role="status"
             hidden></div>

        <div class="tickets-toolbar">
            <input id="ticketBusqueda"
                   type="text"
                   class="tickets-input"
                   placeholder="Buscar por ID, título, descripción o categoría"
                   autocomplete="off"
                   aria-label="Buscar tickets">

            <select id="ticketEstado"
                    class="tickets-select"
                    aria-label="Filtrar por estado">
                <option value="">Todos los estados</option>
                <option value="PENDIENTE">Pendientes</option>
                <option value="EN_PROCESO">En proceso</option>
                <option value="RESUELTO">Resueltos</option>
                <option value="CERRADO">Cerrados</option>
            </select>

            <select id="ticketCategoriaFiltro"
                    class="tickets-select"
                    aria-label="Filtrar por categoría">
                <option value="">Todas las categorías</option>
            </select>

            <button id="ticketActualizarBtn"
                    class="button button-secondary"
                    type="button">
                Actualizar
            </button>
        </div>

        <div class="content-panel">
            <div class="panel-heading">
                <div>
                    <h2>Solicitudes registradas</h2>
                    <p>Tickets disponibles para tu cuenta.</p>
                </div>
                <span class="tickets-count"
                      id="ticketContador">Sin datos</span>
            </div>

            <div class="table-scroll">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Título</th>
                            <th>Categoría</th>
                            <th>Estado</th>
                            <th>Fecha</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody id="ticketsTabla">
                        <tr>
                            <td colspan="6" class="tickets-empty">
                                Consultando tickets...
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <p class="tickets-caption">
            Solo pueden modificarse tickets pendientes.
            Para eliminarlos, tampoco deben tener historial.
        </p>
    `;

    const overlay = document.createElement("div");
    overlay.className = "tickets-modal-overlay";
    overlay.hidden = true;
    document.body.appendChild(overlay);

    const nuevoBtn = $("ticketNuevoBtn");
    const actualizarBtn = $("ticketActualizarBtn");
    const busqueda = $("ticketBusqueda");
    const estadoFiltro = $("ticketEstado");
    const categoriaFiltro = $("ticketCategoriaFiltro");
    const tabla = $("ticketsTabla");
    const contador = $("ticketContador");
    const feedback = $("ticketsFeedback");

    function mensaje(texto, error = false) {
        feedback.textContent = texto;
        feedback.classList.toggle("error", error);
        feedback.hidden = !texto;
    }

    function normalizar(valor) {
        return String(valor == null ? "" : valor)
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim();
    }

    function agregarCelda(fila, valor) {
        const celda = document.createElement("td");
        celda.textContent =
            valor == null ? "" : String(valor);
        fila.appendChild(celda);
        return celda;
    }

    function mostrarVacio(texto) {
        tabla.replaceChildren();

        const fila = document.createElement("tr");
        const celda = agregarCelda(fila, texto);

        celda.colSpan = 6;
        celda.className = "tickets-empty";
        tabla.appendChild(fila);
    }

    function estadoVisual(estado) {
        const valor = String(estado || "").toUpperCase();

        if (valor === "PENDIENTE") {
            return "pending";
        }

        if (valor === "EN_PROCESO") {
            return "progress";
        }

        if (valor === "RESUELTO" ||
            valor === "CERRADO") {
            return "resolved";
        }

        return "";
    }

    function crearBoton(texto, accion, peligro = false) {
        const boton = document.createElement("button");

        boton.type = "button";
        boton.className =
            "tickets-action" + (peligro ? " danger" : "");
        boton.textContent = texto;
        boton.addEventListener("click", accion);

        return boton;
    }

    function renderizarTabla() {

        const texto = normalizar(busqueda.value);
        const estado = estadoFiltro.value;
        const categoriaId = categoriaFiltro.value;

        const filtrados = tickets.filter(ticket => {

            const contenido = normalizar([
                ticket.id,
                ticket.titulo,
                ticket.descripcion,
                ticket.categoria,
                ticket.estado,
                ticket.fecha
            ].join(" "));

            const coincideTexto = contenido.includes(texto);

            const coincideEstado =
                !estado ||
                String(ticket.estado).toUpperCase() === estado;

            const coincideCategoria =
                !categoriaId ||
                String(ticket.categoriaId) === categoriaId;

            return coincideTexto &&
                coincideEstado &&
                coincideCategoria;
        });

        contador.textContent =
            `${filtrados.length} de ${tickets.length} tickets`;

        tabla.replaceChildren();

        if (!filtrados.length) {
            mostrarVacio(
                tickets.length
                    ? "No hay tickets que coincidan con los filtros."
                    : "No hay tickets registrados."
            );
            return;
        }

        filtrados.forEach(ticket => {

            const fila = document.createElement("tr");

            agregarCelda(fila, ticket.id);
            agregarCelda(fila, ticket.titulo);
            agregarCelda(fila, ticket.categoria);

            const celdaEstado =
                agregarCelda(fila, "");

            const etiqueta = document.createElement("span");
            etiqueta.className =
                "tickets-status " +
                estadoVisual(ticket.estado);
            etiqueta.textContent = ticket.estado;

            celdaEstado.appendChild(etiqueta);

            agregarCelda(fila, ticket.fecha);

            const acciones = agregarCelda(fila, "");

            if (String(ticket.estado).toUpperCase()
                === "PENDIENTE") {

                acciones.appendChild(
                    crearBoton(
                        "Editar",
                        () => abrirFormulario(ticket)
                    )
                );

                acciones.appendChild(
                    crearBoton(
                        "Eliminar",
                        () => confirmarEliminar(ticket),
                        true
                    )
                );

            } else {
                acciones.textContent = "Solo consulta";
            }

            tabla.appendChild(fila);
        });
    }

    function renderizarCategorias() {
        const seleccion = categoriaFiltro.value;

        categoriaFiltro.replaceChildren();

        const opcionTodas =
            document.createElement("option");
        opcionTodas.value = "";
        opcionTodas.textContent = "Todas las categorías";
        categoriaFiltro.appendChild(opcionTodas);

        categorias.forEach(categoria => {
            const opcion = document.createElement("option");
            opcion.value = String(categoria.id);
            opcion.textContent = categoria.nombre;
            categoriaFiltro.appendChild(opcion);
        });

        categoriaFiltro.value = seleccion;

        if (categoriaFiltro.selectedIndex < 0) {
            categoriaFiltro.value = "";
        }
    }

    function actualizarResumen() {

        const escribir = (id, valor) => {
            const elemento = $(id);
            if (elemento) {
                elemento.textContent = String(valor);
            }
        };

        escribir("totalTickets", tickets.length);

        escribir(
            "pendingTickets",
            tickets.filter(t =>
                String(t.estado).toUpperCase() === "PENDIENTE"
            ).length
        );

        escribir(
            "activeTickets",
            tickets.filter(t =>
                String(t.estado).toUpperCase() === "EN_PROCESO"
            ).length
        );

        escribir(
            "resolvedTickets",
            tickets.filter(t =>
                ["RESUELTO", "CERRADO"].includes(
                    String(t.estado).toUpperCase()
                )
            ).length
        );

        const recientes = $("recentTickets");

        if (!recientes) {
            return;
        }

        recientes.replaceChildren();

        const ultimos = [...tickets]
            .sort((a, b) => b.id - a.id)
            .slice(0, 5);

        if (!ultimos.length) {
            const fila = document.createElement("tr");
            const celda = agregarCelda(
                fila,
                "No hay solicitudes disponibles."
            );
            celda.colSpan = 5;
            celda.className = "empty-state";
            recientes.appendChild(fila);
            return;
        }

        ultimos.forEach(ticket => {
            const fila = document.createElement("tr");

            agregarCelda(fila, ticket.id);
            agregarCelda(fila, ticket.titulo);
            agregarCelda(fila, ticket.categoria);
            agregarCelda(fila, ticket.estado);
            agregarCelda(fila, ticket.fecha);

            recientes.appendChild(fila);
        });
    }

    function consultar() {

        if (consultando || operando) {
            return;
        }

        consultando = true;

        mostrarVacio("Consultando tickets...");

        try {
            puente.listarDatos();

        } catch (error) {
            consultando = false;
            mensaje(
                "No se pudo iniciar la consulta de tickets.",
                true
            );
        }
    }

    function cerrarModal() {

        if (operando) {
            return;
        }

        overlay.hidden = true;
        overlay.replaceChildren();
        modal = null;
    }

    function abrirModal(titulo, descripcion) {

        overlay.replaceChildren();
        overlay.hidden = false;

        const cuadro = document.createElement("div");
        cuadro.className = "tickets-modal";
        cuadro.setAttribute("role", "dialog");
        cuadro.setAttribute("aria-modal", "true");

        const encabezado = document.createElement("h2");
        encabezado.textContent = titulo;

        const explicacion = document.createElement("p");
        explicacion.textContent = descripcion;

        cuadro.append(encabezado, explicacion);
        overlay.appendChild(cuadro);

        modal = cuadro;
        return cuadro;
    }

    function campo(
        formulario,
        etiquetaTexto,
        tipo,
        valor = "",
        obligatorio = true
    ) {

        const etiqueta = document.createElement("label");
        etiqueta.textContent = etiquetaTexto;

        const entrada = document.createElement("input");
        entrada.type = tipo;
        entrada.className = "tickets-input";
        entrada.required = obligatorio;
        entrada.value = valor;

        etiqueta.appendChild(entrada);
        formulario.appendChild(etiqueta);

        return entrada;
    }

    function botonesFormulario(
        formulario,
        tituloConfirmacion
    ) {

        const error = document.createElement("div");
        error.className = "tickets-modal-error";

        const acciones = document.createElement("div");
        acciones.className = "tickets-modal-actions";

        const cancelar = document.createElement("button");
        cancelar.type = "button";
        cancelar.className = "button button-secondary";
        cancelar.textContent = "Cancelar";
        cancelar.addEventListener("click", cerrarModal);

        const confirmar = document.createElement("button");
        confirmar.type = "submit";
        confirmar.className = "button button-primary";
        confirmar.textContent = tituloConfirmacion;

        acciones.append(cancelar, confirmar);
        formulario.append(error, acciones);

        return error;
    }

    function enviarAccion(accion, datos, error) {

        if (operando || consultando) {
            return;
        }

        operando = true;
        error.textContent = "Guardando cambios...";

        try {
            puente.ejecutarAccion(
                accion,
                JSON.stringify(datos)
            );

        } catch (excepcion) {
            operando = false;
            error.textContent =
                "No se pudo comunicar con el servicio de tickets.";
        }
    }

    function abrirFormulario(ticket = null) {

        const edicion = ticket !== null;

        const cuadro = abrirModal(
            edicion ? "Editar ticket" : "Nuevo ticket",
            edicion
                ? "Solo se permiten cambios mientras el ticket esté pendiente."
                : "Completa los datos de la solicitud de soporte."
        );

        const formulario = document.createElement("form");
        formulario.className = "tickets-form";

        const titulo = campo(
            formulario,
            "Título",
            "text",
            edicion ? ticket.titulo : ""
        );

        titulo.maxLength = 150;

        const etiquetaDescripcion =
            document.createElement("label");
        etiquetaDescripcion.textContent = "Descripción";

        const descripcion =
            document.createElement("textarea");
        descripcion.className = "tickets-textarea";
        descripcion.required = true;
        descripcion.value =
            edicion ? ticket.descripcion : "";

        etiquetaDescripcion.appendChild(descripcion);
        formulario.appendChild(etiquetaDescripcion);

        const etiquetaCategoria =
            document.createElement("label");
        etiquetaCategoria.textContent = "Categoría";

        const categoria = document.createElement("select");
        categoria.className = "tickets-select";
        categoria.required = true;

        const inicial = document.createElement("option");
        inicial.value = "";
        inicial.textContent = "Selecciona una categoría";
        categoria.appendChild(inicial);

        categorias.forEach(item => {
            const opcion = document.createElement("option");
            opcion.value = String(item.id);
            opcion.textContent = item.nombre;
            categoria.appendChild(opcion);
        });

        if (edicion) {
            categoria.value = String(ticket.categoriaId);
        }

        etiquetaCategoria.appendChild(categoria);
        formulario.appendChild(etiquetaCategoria);

        const error = botonesFormulario(
            formulario,
            edicion ? "Guardar cambios" : "Registrar ticket"
        );

        formulario.addEventListener("submit", evento => {

            evento.preventDefault();

            if (!formulario.reportValidity()) {
                return;
            }

            const datos = {
                titulo: titulo.value.trim(),
                descripcion: descripcion.value.trim(),
                categoriaId: Number(categoria.value)
            };

            if (edicion) {
                datos.id = ticket.id;
            }

            if (!datos.titulo ||
                !datos.descripcion ||
                !datos.categoriaId) {
                error.textContent =
                    "Completa los datos obligatorios.";
                return;
            }

            enviarAccion(
                edicion ? "actualizar" : "crear",
                datos,
                error
            );
        });

        cuadro.appendChild(formulario);
        titulo.focus();
    }

    function confirmarEliminar(ticket) {

        const cuadro = abrirModal(
            "Eliminar ticket",
            "Esta acción es permanente y solamente " +
            "se permite para tickets pendientes sin historial."
        );

        const texto = document.createElement("p");
        texto.textContent =
            `¿Deseas eliminar el ticket #${ticket.id}: ` +
            `${ticket.titulo}?`;

        const formulario = document.createElement("form");
        formulario.className = "tickets-form";

        const error = botonesFormulario(
            formulario,
            "Eliminar ticket"
        );

        formulario.addEventListener("submit", evento => {
            evento.preventDefault();

            enviarAccion(
                "eliminar",
                { id: ticket.id },
                error
            );
        });

        cuadro.append(texto, formulario);
    }

    window.nautaTicketsDataReady = datos => {

        consultando = false;

        tickets = Array.isArray(datos.tickets)
            ? datos.tickets
            : [];

        categorias = Array.isArray(datos.categorias)
            ? datos.categorias
            : [];

        renderizarCategorias();
        renderizarTabla();
        actualizarResumen();
    };

    window.nautaTicketsDataError = texto => {

        consultando = false;

        mostrarVacio(
            "No se pudieron consultar los tickets."
        );

        mensaje(
            texto || "Error de consulta.",
            true
        );
    };

    window.nautaTicketsActionResult = resultado => {

        operando = false;

        if (!resultado.correcto) {

            const error = modal
                ? modal.querySelector(".tickets-modal-error")
                : null;

            if (error) {
                error.textContent =
                    resultado.mensaje || "Operación fallida.";
            }

            mensaje(
                resultado.mensaje || "Operación fallida.",
                true
            );
            return;
        }

        cerrarModal();

        mensaje(
            resultado.mensaje || "Operación realizada."
        );

        consultar();
    };

    nuevoBtn.addEventListener("click", () => {
        abrirFormulario();
    });

    actualizarBtn.addEventListener("click", consultar);

    busqueda.addEventListener("input", renderizarTabla);

    estadoFiltro.addEventListener(
        "change",
        renderizarTabla
    );

    categoriaFiltro.addEventListener(
        "change",
        renderizarTabla
    );

    consultar();
})();
