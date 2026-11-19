
document.addEventListener("DOMContentLoaded", () => {

    const $ = id => document.getElementById(id);

    const nombres = {
        resumen: "Resumen",
        tickets: "Tickets",
        atencion: "Atención",
        historial: "Historial",
        reportes: "Reportes",
        administracion: "Administración"
    };

    let sesionConectada = false;
    let perfilActual = null;
    let cargando = false;
    let operando = false;

    let usuarios = [];
    let roles = [];
    let departamentos = [];
    let filtroUsuarios = "";

    const botonNuevoUsuario = $("nuevoUsuarioBtn");
    const botonNuevoDepartamento = $("nuevoDepartamentoBtn");

    // Estilos compatibles con JavaFX WebView.
    const estilo = document.createElement("style");

    estilo.textContent = `
        html, body {
            color-scheme: dark;
        }

        .admin-toolbar {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
            padding: 17px 22px;
            border-bottom: 1px solid #292929;
            background: #0d0d0d;
        }

        .admin-search,
        .admin-field {
            -webkit-appearance: none !important;
            appearance: none !important;
            box-sizing: border-box;
            color-scheme: dark !important;
            color: #f2f2f4 !important;
            background: #080808 !important;
            background-color: #080808 !important;
            border: 1px solid #333333 !important;
            border-radius: 6px;
            outline: none;
            box-shadow: none;
            -webkit-text-fill-color: #f2f2f4;
            font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
            font-size: 12px;
        }

        .admin-search {
            width: 100%;
            max-width: 430px;
            height: 42px;
            padding: 0 14px;
        }

        .admin-search::placeholder,
        .admin-field::placeholder {
            color: #92929a !important;
            -webkit-text-fill-color: #92929a;
            opacity: 1;
        }

        .admin-search:focus,
        .admin-field:focus {
            color: #ffffff !important;
            background: #0a0a0a !important;
            background-color: #0a0a0a !important;
            border-color: #9783db !important;
            box-shadow: 0 0 0 2px rgba(151, 131, 219, .15);
            -webkit-text-fill-color: #ffffff;
        }

        .admin-search::-webkit-search-decoration,
        .admin-search::-webkit-search-cancel-button,
        .admin-search::-webkit-search-results-button,
        .admin-search::-webkit-search-results-decoration {
            -webkit-appearance: none;
            appearance: none;
        }

        .admin-search:-webkit-autofill,
        .admin-search:-webkit-autofill:hover,
        .admin-search:-webkit-autofill:focus {
            -webkit-box-shadow:
                0 0 0 1000px #080808 inset !important;
            -webkit-text-fill-color: #f2f2f4 !important;
        }

        .admin-filter-count {
            color: #a6a6ae;
            font-size: 12px;
        }

        .admin-small-button {
            padding: 7px 11px;
            margin-right: 6px;
            color: #d3c5f5;
            background: #1a1522;
            border: 1px solid #3a3048;
            border-radius: 5px;
            font-size: 11px;
            cursor: pointer;
        }

        .admin-small-button:hover {
            background: #292039;
        }

        .admin-small-button.danger {
            color: #e4a2ac;
            background: #1c1215;
            border-color: #513038;
        }

        .admin-small-button:disabled {
            opacity: .4;
            cursor: not-allowed;
        }

        .admin-modal-backdrop {
            position: fixed;
            inset: 0;
            z-index: 5000;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            background: rgba(0, 0, 0, .85);
        }

        .admin-modal-backdrop[hidden] {
            display: none !important;
        }

        .admin-modal {
            width: 100%;
            max-width: 520px;
            max-height: 92vh;
            overflow-y: auto;
            padding: 26px;
            color: #f2f2f4;
            background: #101010;
            border: 1px solid #343434;
            border-radius: 10px;
            font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
        }

        .admin-modal h2 {
            margin: 0 0 8px;
            font-size: 19px;
            font-weight: 600;
        }

        .admin-modal p {
            margin: 0 0 20px;
            color: #a4a4ab;
            font-size: 12px;
            line-height: 1.6;
        }

        .admin-form {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 15px;
        }

        .admin-form-label {
            display: flex;
            flex-direction: column;
            gap: 7px;
            min-width: 0;
            color: #d2d2d7;
            font-size: 12px;
        }

        .admin-form-label.wide {
            grid-column: 1 / -1;
        }

        .admin-field {
            width: 100%;
            min-height: 41px;
            padding: 10px 12px;
        }

        select.admin-field {
            -webkit-appearance: auto !important;
            appearance: auto !important;
            background-color: #080808 !important;
            color: #f2f2f4 !important;
        }

        select.admin-field option {
            background: #111111 !important;
            color: #f2f2f4 !important;
        }

        .admin-form textarea {
            resize: vertical;
            min-height: 78px;
        }

        .admin-modal-actions {
            grid-column: 1 / -1;
            display: flex;
            justify-content: flex-end;
            flex-wrap: wrap;
            gap: 10px;
            margin-top: 8px;
        }

        .admin-modal-status {
            grid-column: 1 / -1;
            min-height: 16px;
            color: #e4a2ac;
            font-size: 12px;
        }

        .admin-feedback {
            margin: 0 0 17px;
            padding: 12px 15px;
            color: #c9baf4;
            background: #141018;
            border: 1px solid #3c304d;
            border-radius: 6px;
            font-size: 12px;
        }

        .admin-feedback.error {
            color: #e4a2ac;
            background: #1a1013;
            border-color: #4a2830;
        }

        @media (max-width: 560px) {
            .admin-form {
                grid-template-columns: 1fr;
            }

            .admin-form-label.wide,
            .admin-modal-actions,
            .admin-modal-status {
                grid-column: 1;
            }

            .admin-search {
                max-width: none;
            }
        }
    `;

    document.head.appendChild(estilo);

    const areaAdmin = $("administracion");
    const panelUsuarios = $("adminUsuarios");

    // Barra de búsqueda de usuarios.
    const barraFiltro = document.createElement("div");
    barraFiltro.className = "admin-toolbar";

    const buscar = document.createElement("input");

    // Usamos text para evitar controles nativos de
    // búsqueda que JavaFX WebView pinta en blanco.
    buscar.type = "text";
    buscar.className = "admin-search";
    buscar.placeholder =
        "Buscar por nombre, correo, rol, departamento o estado";

    buscar.autocomplete = "off";
    buscar.spellcheck = false;

    buscar.setAttribute(
        "aria-label",
        "Filtrar usuarios"
    );

    const contadorFiltro = document.createElement("span");
    contadorFiltro.className = "admin-filter-count";

    barraFiltro.append(buscar, contadorFiltro);

    const tablaUsuarios = panelUsuarios
        ? panelUsuarios.querySelector(".table-scroll")
        : null;

    if (tablaUsuarios) {
        tablaUsuarios.parentNode.insertBefore(
            barraFiltro,
            tablaUsuarios
        );
    }

    const mensajeAdmin = document.createElement("div");
    mensajeAdmin.className = "admin-feedback";
    mensajeAdmin.hidden = true;

    if (areaAdmin) {
        const info = areaAdmin.querySelector(".admin-info");

        if (info) {
            info.hidden = true;

            info.insertAdjacentElement(
                "afterend",
                mensajeAdmin
            );
        } else {
            areaAdmin.prepend(mensajeAdmin);
        }
    }

    const capaModal = document.createElement("div");
    capaModal.className = "admin-modal-backdrop";
    capaModal.hidden = true;

    document.body.appendChild(capaModal);

    function informar(texto, error = false) {
        mensajeAdmin.textContent = texto;
        mensajeAdmin.classList.toggle("error", error);
        mensajeAdmin.hidden = !texto;
    }

    function permitido(elemento) {

        if (!elemento) {
            return false;
        }

        const declaracion = elemento.dataset.roles;

        if (!declaracion) {
            return true;
        }

        if (!sesionConectada) {
            return false;
        }

        return declaracion
            .split(",")
            .map(valor => valor.trim())
            .includes(perfilActual);
    }

    function aplicarPermisos() {

        document.querySelectorAll("[data-roles]")
            .forEach(elemento => {

                if (!elemento.classList.contains(
                    "workspace-section"
                )) {
                    elemento.hidden = !permitido(elemento);
                }
            });

        const salir = $("cerrarSesionBtn");

        if (salir) {
            salir.hidden = !sesionConectada;
        }

        const adminDisponible =
            sesionConectada &&
            perfilActual === "Administrador" &&
            !!window.nautaAdminBridge;

        if (botonNuevoUsuario) {
            botonNuevoUsuario.disabled = !adminDisponible;
        }

        if (botonNuevoDepartamento) {
            botonNuevoDepartamento.disabled =
                !adminDisponible;
        }
    }

    function mostrarSeccion(nombre) {

        const objetivo = $(nombre);

        if (!Object.prototype.hasOwnProperty.call(
            nombres,
            nombre
        )) {
            return;
        }

        if (!objetivo || !permitido(objetivo)) {
            return;
        }

        document.querySelectorAll(".workspace-section")
            .forEach(seccion => {
                seccion.hidden = seccion !== objetivo;
            });

        document.querySelectorAll("[data-section]")
            .forEach(boton => {

                const activo =
                    boton.dataset.section === nombre;

                boton.classList.toggle("active", activo);

                if (activo) {
                    boton.setAttribute(
                        "aria-current",
                        "page"
                    );
                } else {
                    boton.removeAttribute("aria-current");
                }
            });

        if ($("currentSection")) {
            $("currentSection").textContent = nombres[nombre];
        }

        window.dispatchEvent(new CustomEvent("nautaSeccionCambiada", {
            detail: { seccion: nombre }
        }));

        if (nombre === "administracion") {
            cargarAdministracion();
        }
    }

    function mostrarTab(nombre) {

        if (perfilActual !== "Administrador") {
            return;
        }

        if ($("adminUsuarios")) {
            $("adminUsuarios").hidden =
                nombre !== "usuarios";
        }

        if ($("adminDepartamentos")) {
            $("adminDepartamentos").hidden =
                nombre !== "departamentos";
        }

        document.querySelectorAll("[data-admin-tab]")
            .forEach(boton => {

                const activo =
                    boton.dataset.adminTab === nombre;

                boton.classList.toggle("active", activo);

                boton.setAttribute(
                    "aria-selected",
                    String(activo)
                );
            });
    }

    function agregarCelda(fila, valor) {

        const celda = document.createElement("td");

        celda.textContent =
            valor == null ? "" : String(valor);

        fila.appendChild(celda);

        return celda;
    }

    function mensajeTabla(id, texto, columnas) {

        const cuerpo = $(id);

        if (!cuerpo) {
            return;
        }

        cuerpo.replaceChildren();

        const fila = document.createElement("tr");
        const celda = agregarCelda(fila, texto);

        celda.colSpan = columnas;
        celda.className = "empty-state";

        cuerpo.appendChild(fila);
    }

    function normalizar(texto) {

        return String(texto == null ? "" : texto)
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim();
    }

    function mostrarUsuarios() {

        const cuerpo = $("adminUsuariosBody");

        if (!cuerpo) {
            return;
        }

        cuerpo.replaceChildren();

        const busqueda = normalizar(filtroUsuarios);

        const encontrados = usuarios.filter(usuario => {

            const campos = [
                usuario.id,
                usuario.nombre,
                usuario.apellido,
                usuario.correo,
                usuario.rol,
                usuario.departamento,
                usuario.activo ? "activo" : "inactivo"
            ].join(" ");

            return normalizar(campos).includes(busqueda);
        });

        contadorFiltro.textContent =
            `${encontrados.length} de ${usuarios.length} usuarios`;

        if (!encontrados.length) {

            mensajeTabla(
                "adminUsuariosBody",
                usuarios.length
                    ? "No hay usuarios que coincidan con el filtro."
                    : "No hay usuarios registrados.",
                7
            );

            return;
        }

        encontrados.forEach(usuario => {

            const fila = document.createElement("tr");

            agregarCelda(fila, usuario.id);

            agregarCelda(
                fila,
                [usuario.nombre, usuario.apellido]
                    .filter(Boolean)
                    .join(" ")
            );

            agregarCelda(fila, usuario.correo);
            agregarCelda(fila, usuario.rol);
            agregarCelda(fila, usuario.departamento);

            agregarCelda(
                fila,
                usuario.activo ? "Activo" : "Inactivo"
            );

            const acciones = agregarCelda(fila, "");

            const editar = document.createElement("button");

            editar.type = "button";
            editar.className = "admin-small-button";
            editar.textContent = "Editar";

            editar.addEventListener("click", () => {
                abrirFormularioUsuario(usuario);
            });

            acciones.appendChild(editar);

            if (usuario.activo) {

                const desactivar =
                    document.createElement("button");

                desactivar.type = "button";
                desactivar.className =
                    "admin-small-button danger";

                desactivar.textContent = "Desactivar";

                desactivar.addEventListener("click", () => {
                    confirmarDesactivacion(usuario);
                });

                acciones.appendChild(desactivar);
            }

            cuerpo.appendChild(fila);
        });
    }

    function mostrarDepartamentos() {

        const cuerpo = $("adminDepartamentosBody");

        if (!cuerpo) {
            return;
        }

        cuerpo.replaceChildren();

        if (!departamentos.length) {

            mensajeTabla(
                "adminDepartamentosBody",
                "No hay departamentos registrados.",
                3
            );

            return;
        }

        departamentos.forEach(departamento => {

            const fila = document.createElement("tr");

            agregarCelda(fila, departamento.id);
            agregarCelda(fila, departamento.nombre);
            agregarCelda(fila, "Consulta");

            cuerpo.appendChild(fila);
        });
    }

    function cargarAdministracion() {

        if (!sesionConectada ||
            perfilActual !== "Administrador" ||
            cargando ||
            operando) {
            return;
        }

        const puente = window.nautaAdminBridge;

        if (!puente) {

            informar(
                "El servicio administrativo no está disponible.",
                true
            );

            return;
        }

        cargando = true;

        mensajeTabla(
            "adminUsuariosBody",
            "Cargando usuarios...",
            7
        );

        mensajeTabla(
            "adminDepartamentosBody",
            "Cargando departamentos...",
            3
        );

        try {
            puente.listarDatos();

        } catch (error) {

            cargando = false;

            informar(
                "No se pudo iniciar la consulta administrativa.",
                true
            );
        }
    }

    function cerrarModal() {

        if (operando) {
            return;
        }

        capaModal.replaceChildren();
        capaModal.hidden = true;
    }

    function abrirModal(titulo, descripcion) {

        capaModal.replaceChildren();
        capaModal.hidden = false;

        const dialogo = document.createElement("div");

        dialogo.className = "admin-modal";
        dialogo.setAttribute("role", "dialog");
        dialogo.setAttribute("aria-modal", "true");

        const encabezado = document.createElement("h2");
        encabezado.textContent = titulo;

        const subtitulo = document.createElement("p");
        subtitulo.textContent = descripcion;

        dialogo.append(encabezado, subtitulo);
        capaModal.appendChild(dialogo);

        return dialogo;
    }

    function campoFormulario(
        formulario,
        titulo,
        nombre,
        tipo,
        valor,
        requerido = false,
        ancho = false
    ) {

        const etiqueta = document.createElement("label");

        etiqueta.className =
            "admin-form-label" + (ancho ? " wide" : "");

        etiqueta.textContent = titulo;

        const campo = document.createElement("input");

        campo.className = "admin-field";
        campo.type = tipo;
        campo.name = nombre;
        campo.value = valor == null ? "" : String(valor);
        campo.required = requerido;

        etiqueta.appendChild(campo);
        formulario.appendChild(etiqueta);

        return campo;
    }

    function selectorFormulario(
        formulario,
        titulo,
        nombre,
        opciones,
        seleccionado
    ) {

        const etiqueta = document.createElement("label");

        etiqueta.className = "admin-form-label";
        etiqueta.textContent = titulo;

        const selector = document.createElement("select");

        selector.className = "admin-field";
        selector.name = nombre;
        selector.required = true;

        const inicial = document.createElement("option");

        inicial.value = "";
        inicial.textContent = "Seleccionar...";

        selector.appendChild(inicial);

        opciones.forEach(opcion => {

            const item = document.createElement("option");

            item.value = String(opcion.id);
            item.textContent = opcion.nombre;

            selector.appendChild(item);
        });

        if (seleccionado != null) {
            selector.value = String(seleccionado);
        }

        etiqueta.appendChild(selector);
        formulario.appendChild(etiqueta);

        return selector;
    }

    function accionesFormulario(
        formulario,
        textoBoton
    ) {

        const estado = document.createElement("div");
        estado.className = "admin-modal-status";

        const acciones = document.createElement("div");
        acciones.className = "admin-modal-actions";

        const cancelar = document.createElement("button");

        cancelar.type = "button";
        cancelar.className = "button button-secondary";
        cancelar.textContent = "Cancelar";

        cancelar.addEventListener("click", cerrarModal);

        const guardar = document.createElement("button");

        guardar.type = "submit";
        guardar.className = "button button-primary";
        guardar.textContent = textoBoton;

        acciones.append(cancelar, guardar);
        formulario.append(estado, acciones);

        return estado;
    }

    function enviarAccion(accion, datos, estado) {

        if (operando) {
            return;
        }

        const puente = window.nautaAdminBridge;

        if (!puente ||
            !sesionConectada ||
            perfilActual !== "Administrador") {

            estado.textContent =
                "No hay una sesión administrativa disponible.";

            return;
        }

        operando = true;
        estado.textContent = "Guardando cambios...";

        try {

            puente.ejecutarAccion(
                accion,
                JSON.stringify(datos)
            );

        } catch (error) {

            operando = false;

            estado.textContent =
                "No fue posible enviar la operación.";
        }
    }

    function abrirFormularioUsuario(usuario = null) {

        if (!sesionConectada ||
            perfilActual !== "Administrador") {
            return;
        }

        const edicion = usuario !== null;

        const dialogo = abrirModal(
            edicion ? "Editar usuario" : "Nuevo usuario",
            edicion
                ? "Modifica los datos permitidos. Deja la contraseña vacía para conservarla."
                : "Completa la información de la nueva cuenta."
        );

        const formulario = document.createElement("form");
        formulario.className = "admin-form";

        const nombre = campoFormulario(
            formulario,
            "Nombre",
            "nombre",
            "text",
            usuario?.nombre,
            true
        );

        const apellido = campoFormulario(
            formulario,
            "Apellido",
            "apellido",
            "text",
            usuario?.apellido,
            true
        );

        const correo = campoFormulario(
            formulario,
            "Correo electrónico",
            "correo",
            "email",
            usuario?.correo,
            true,
            true
        );

        const password = campoFormulario(
            formulario,
            edicion
                ? "Nueva contraseña (opcional)"
                : "Contraseña",
            "password",
            "password",
            "",
            !edicion,
            true
        );

        password.autocomplete = "new-password";
        password.minLength = 8;

        // Solo se pueden registrar empleados y técnicos.
        // El administrador existente conserva su rol al editar sus datos.
        const rolActual = roles.find(opcion =>
            String(opcion.id) === String(usuario?.rolId)
        );

        const esAdminActual = edicion &&
            String(rolActual?.nombre ?? "").trim().toUpperCase() === "ADMIN";

        const rolesDisponibles = esAdminActual
            ? [rolActual]
            : roles.filter(opcion =>
                String(opcion.nombre ?? "").trim().toUpperCase() !== "ADMIN"
            );

        const rol = selectorFormulario(
            formulario,
            "Rol",
            "rolId",
            rolesDisponibles,
            usuario?.rolId
        );

        if (esAdminActual) {
            rol.disabled = true;
        }

        const departamento = selectorFormulario(
            formulario,
            "Departamento",
            "departamentoId",
            departamentos,
            usuario?.departamentoId
        );

        const estado = accionesFormulario(
            formulario,
            edicion ? "Guardar cambios" : "Crear usuario"
        );

        formulario.addEventListener("submit", evento => {

            evento.preventDefault();

            if (!formulario.reportValidity()) {
                return;
            }

            if (password.value &&
                password.value.length < 8) {

                estado.textContent =
                    "La contraseña requiere al menos 8 caracteres.";

                return;
            }

            const datos = {
                nombre: nombre.value.trim(),
                apellido: apellido.value.trim(),
                correo: correo.value.trim(),
                password: password.value,
                rolId: Number(rol.value),
                departamentoId: Number(departamento.value)
            };

            if (edicion) {
                datos.id = usuario.id;
            }

            enviarAccion(
                edicion ? "editarUsuario" : "crearUsuario",
                datos,
                estado
            );

            password.value = "";
        });

        dialogo.appendChild(formulario);
        nombre.focus();
    }

    function abrirFormularioDepartamento() {

        if (!sesionConectada ||
            perfilActual !== "Administrador") {
            return;
        }

        const dialogo = abrirModal(
            "Nuevo departamento",
            "Registra un departamento en NautaTicket."
        );

        const formulario = document.createElement("form");
        formulario.className = "admin-form";

        const nombre = campoFormulario(
            formulario,
            "Nombre",
            "nombre",
            "text",
            "",
            true,
            true
        );

        nombre.maxLength = 100;

        const etiqueta = document.createElement("label");

        etiqueta.className = "admin-form-label wide";
        etiqueta.textContent = "Descripción";

        const descripcion = document.createElement("textarea");

        descripcion.className = "admin-field";
        descripcion.name = "descripcion";

        etiqueta.appendChild(descripcion);
        formulario.appendChild(etiqueta);

        const estado = accionesFormulario(
            formulario,
            "Crear departamento"
        );

        formulario.addEventListener("submit", evento => {

            evento.preventDefault();

            if (!formulario.reportValidity()) {
                return;
            }

            enviarAccion(
                "crearDepartamento",
                {
                    nombre: nombre.value.trim(),
                    descripcion: descripcion.value.trim()
                },
                estado
            );
        });

        dialogo.appendChild(formulario);
        nombre.focus();
    }

    function confirmarDesactivacion(usuario) {

        const dialogo = abrirModal(
            "Desactivar usuario",
            "La cuenta dejará de estar activa. Esta operación no podrá revertirse desde este panel."
        );

        const nombreCompleto =
            [usuario.nombre, usuario.apellido]
                .filter(Boolean)
                .join(" ");

        const texto = document.createElement("p");

        texto.textContent =
            `¿Deseas desactivar a ${nombreCompleto}?`;

        const formulario = document.createElement("form");
        formulario.className = "admin-form";

        const estado = accionesFormulario(
            formulario,
            "Desactivar usuario"
        );

        formulario.addEventListener("submit", evento => {

            evento.preventDefault();

            enviarAccion(
                "desactivarUsuario",
                { id: usuario.id },
                estado
            );
        });

        dialogo.append(texto, formulario);
    }

    window.nautaAdminDataReady = datos => {

        cargando = false;

        if (!sesionConectada ||
            perfilActual !== "Administrador") {
            return;
        }

        usuarios = Array.isArray(datos.usuarios)
            ? datos.usuarios
            : [];

        roles = Array.isArray(datos.roles)
            ? datos.roles
            : [];

        departamentos = Array.isArray(datos.departamentos)
            ? datos.departamentos
            : [];

        if ($("adminTotalUsuarios")) {
            $("adminTotalUsuarios").textContent =
                datos.totalUsuarios ?? "—";
        }

        if ($("adminUsuariosActivos")) {
            $("adminUsuariosActivos").textContent =
                datos.usuariosActivos ?? "—";
        }

        if ($("adminTotalDepartamentos")) {
            $("adminTotalDepartamentos").textContent =
                datos.totalDepartamentos ?? "—";
        }

        mostrarUsuarios();
        mostrarDepartamentos();
    };

    window.nautaAdminDataError = mensaje => {

        cargando = false;

        informar(
            mensaje || "Error consultando usuarios.",
            true
        );

        mensajeTabla(
            "adminUsuariosBody",
            "Error al cargar.",
            7
        );

        mensajeTabla(
            "adminDepartamentosBody",
            "Error al cargar.",
            3
        );
    };

    window.nautaAdminActionResult = resultado => {

        operando = false;

        if (!sesionConectada ||
            perfilActual !== "Administrador") {
            return;
        }

        if (resultado.correcto) {

            capaModal.hidden = true;
            capaModal.replaceChildren();

            informar(
                resultado.mensaje || "Operación realizada."
            );

            cargarAdministracion();

        } else {

            const estado = capaModal.querySelector(
                ".admin-modal-status"
            );

            if (estado) {
                estado.textContent =
                    resultado.mensaje ||
                    "La operación falló.";
            }

            informar(
                resultado.mensaje || "La operación falló.",
                true
            );
        }
    };

    window.nautaDashboardReady = () => {

        const puente = window.nautaDashboardBridge;

        if (!puente) {

            sesionConectada = false;
            perfilActual = null;

            aplicarPermisos();
            mostrarSeccion("resumen");

            return;
        }

        try {

            const correo = String(
                puente.getCorreo() ?? ""
            ).trim();

            const perfil = String(
                puente.getPerfil() ?? ""
            ).trim();

            const perfilesValidos = [
                "Administrador",
                "Soporte técnico",
                "Empleado"
            ];

            if (!correo ||
                !perfilesValidos.includes(perfil)) {

                throw new Error(
                    "Sesión no válida"
                );
            }

            sesionConectada = true;
            perfilActual = perfil;

            if ($("userName")) {
                $("userName").textContent = correo;
                $("userName").title = correo;
            }

            if ($("userRole")) {
                $("userRole").textContent = perfil;
            }

            if ($("roleTag")) {
                $("roleTag").textContent = perfil;
            }

            const avatar = document.querySelector(
                ".user-avatar"
            );

            if (avatar) {
                avatar.textContent =
                    correo.charAt(0).toUpperCase();
            }

            aplicarPermisos();
            mostrarSeccion(perfil === "Empleado" ? "tickets"
                : perfil === "Soporte técnico" ? "atencion" : "resumen");

            if (perfil === "Administrador") {
                mostrarTab("usuarios");
            }

        } catch (error) {

            sesionConectada = false;
            perfilActual = null;

            aplicarPermisos();
        }
    };

    // Eventos del filtro.
    buscar.addEventListener("input", () => {

        filtroUsuarios = buscar.value;
        mostrarUsuarios();
    });

    // Navegación.
    document.querySelectorAll("[data-section]")
        .forEach(boton => {

            boton.addEventListener("click", () => {
                mostrarSeccion(
                    boton.dataset.section
                );
            });
        });

    document.querySelectorAll("[data-go]")
        .forEach(boton => {

            boton.addEventListener("click", () => {
                mostrarSeccion(
                    boton.dataset.go
                );
            });
        });

    document.querySelectorAll("[data-admin-tab]")
        .forEach(boton => {

            boton.addEventListener("click", () => {
                mostrarTab(
                    boton.dataset.adminTab
                );
            });
        });

    if (botonNuevoUsuario) {

        botonNuevoUsuario.addEventListener(
            "click",
            () => abrirFormularioUsuario()
        );
    }

    if (botonNuevoDepartamento) {

        botonNuevoDepartamento.addEventListener(
            "click",
            abrirFormularioDepartamento
        );
    }

    const salir = $("cerrarSesionBtn");

    if (salir) {

        salir.addEventListener("click", () => {

            if (!sesionConectada) {
                return;
            }

            try {

                window.nautaDashboardBridge
                    .cerrarSesion();

            } catch (error) {

                informar(
                    "No fue posible cerrar sesión.",
                    true
                );
            }
        });
    }

    mostrarSeccion("resumen");
    aplicarPermisos();

    if (window.nautaDashboardBridge) {
        window.nautaDashboardReady();
    }
});
