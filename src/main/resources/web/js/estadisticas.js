(function () {
    "use strict";

    const seccion = document.getElementById("resumen");
    const puente = window.nautaEstadisticasBridge;

    if (!seccion || !puente) {
        return;
    }

    const estilos = document.createElement("style");

    estilos.textContent = `
        .api-panel {
            margin-top: 24px;
            padding: 24px;
            background: #0d0d0d;
            border: 1px solid #292929;
            border-radius: 9px;
        }

        .api-panel-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 16px;
            margin-bottom: 23px;
        }

        .api-panel-header h2 {
            margin: 0 0 8px;
            color: #f2f2f4;
            font-size: 15px;
            font-weight: 600;
        }

        .api-panel-header p {
            margin: 0;
            color: #9999a3;
            font-size: 12px;
            line-height: 1.7;
        }

        .api-metricas {
            display: grid;
            grid-template-columns: repeat(4, minmax(0, 1fr));
            gap: 12px;
            margin-bottom: 22px;
        }

        .api-metrica {
            padding: 18px;
            background: #141414;
            border: 1px solid #303030;
            border-radius: 7px;
        }

        .api-metrica span {
            display: block;
            color: #a7a7b0;
            font-size: 11px;
        }

        .api-metrica strong {
            display: block;
            margin-top: 12px;
            color: #f2f2f4;
            font-family: "IBM Plex Mono", Consolas, monospace;
            font-size: 24px;
            font-weight: 500;
        }

        .api-grafico {
            display: flex;
            flex-direction: column;
            gap: 17px;
            padding: 21px;
            background: #111111;
            border: 1px solid #292929;
            border-radius: 7px;
        }

        .api-barra-fila {
            display: grid;
            grid-template-columns: 110px minmax(0, 1fr) 40px;
            align-items: center;
            gap: 14px;
        }

        .api-barra-etiqueta {
            color: #b9b9c3;
            font-size: 11px;
        }

        .api-barra-pista {
            height: 13px;
            overflow: hidden;
            background: #272727;
            border-radius: 4px;
        }

        .api-barra-valor {
            width: 0%;
            height: 100%;
            border-radius: 4px;
            background: #9783db;
            transition: width 250ms ease;
        }

        .api-barra-fila:nth-child(2) .api-barra-valor {
            background: #7194d2;
        }

        .api-barra-fila:nth-child(3) .api-barra-valor {
            background: #79b59b;
        }

        .api-barra-fila:nth-child(4) .api-barra-valor {
            background: #b7a0cf;
        }

        .api-barra-numero {
            color: #d9d9e2;
            text-align: right;
            font-family: "IBM Plex Mono", Consolas, monospace;
            font-size: 12px;
        }

        .api-mensaje {
            padding: 14px;
            margin-top: 18px;
            color: #c9baf4;
            background: #17121e;
            border: 1px solid #40304d;
            border-radius: 6px;
            font-size: 12px;
            line-height: 1.7;
        }

        .api-mensaje.error {
            color: #e6a5ad;
            background: #1a1013;
            border-color: #4c2931;
        }

        .api-pie {
            margin-top: 17px;
            color: #85858e;
            font-size: 11px;
        }

        @media (max-width: 1050px) {
            .api-metricas {
                grid-template-columns: repeat(2, 1fr);
            }
        }

        @media (max-width: 600px) {
            .api-metricas {
                grid-template-columns: 1fr;
            }

            .api-barra-fila {
                grid-template-columns: 85px minmax(0,1fr) 30px;
                gap: 8px;
            }
        }
    `;

    document.head.appendChild(estilos);

    const panel = document.createElement("section");
    panel.className = "api-panel";

    panel.innerHTML = `
        <div class="api-panel-header">
            <div>
                <h2>Estadísticas de la API</h2>
                <p>
                    Distribución de tickets por estado
                    obtenida desde NautaTicket API.
                </p>
            </div>

            <button id="apiConsultar"
                    type="button"
                    class="button button-primary">
                Consultar API
            </button>
        </div>

        <div class="api-metricas">
            <article class="api-metrica">
                <span>Pendientes</span>
                <strong id="apiPendientes">—</strong>
            </article>

            <article class="api-metrica">
                <span>En proceso</span>
                <strong id="apiEnProceso">—</strong>
            </article>

            <article class="api-metrica">
                <span>Resueltos</span>
                <strong id="apiResueltos">—</strong>
            </article>

            <article class="api-metrica">
                <span>Cerrados</span>
                <strong id="apiCerrados">—</strong>
            </article>
        </div>

        <div id="apiGrafico" class="api-grafico">
            <p style="color:#9999a3;font-size:12px;margin:0;">
                Consulta la API para mostrar la distribución.
            </p>
        </div>

        <div id="apiMensaje"
             class="api-mensaje"
             role="status"
             hidden></div>

        <p class="api-pie">
            Fuente: API REST de NautaTicket.
            Disponible únicamente para administradores.
        </p>
    `;

    seccion.appendChild(panel);

    const $ = id => document.getElementById(id);

    const botonConsultar = $("apiConsultar");
    const grafico = $("apiGrafico");
    const mensaje = $("apiMensaje");

    const estados = [
        {
            codigo: "PENDIENTE",
            nombre: "Pendientes",
            elemento: "apiPendientes"
        },
        {
            codigo: "EN_PROCESO",
            nombre: "En proceso",
            elemento: "apiEnProceso"
        },
        {
            codigo: "RESUELTO",
            nombre: "Resueltos",
            elemento: "apiResueltos"
        },
        {
            codigo: "CERRADO",
            nombre: "Cerrados",
            elemento: "apiCerrados"
        }
    ];

    function mostrarMensaje(texto, error = false) {
        mensaje.textContent = texto;
        mensaje.hidden = !texto;
        mensaje.classList.toggle("error", error);
    }

    function establecerCarga(cargando) {
        botonConsultar.disabled = cargando;

        botonConsultar.textContent = cargando
            ? "Consultando..."
            : "Consultar API";
    }

    function mostrarDatos(datos) {
        const totales = {};

        estados.forEach(estado => {
            totales[estado.codigo] = 0;
        });

        datos.forEach(item => {
            const codigo = String(
                item.estado || ""
            ).toUpperCase();

            const total = Number(item.total);

            if (Object.prototype.hasOwnProperty.call(
                totales, codigo
            ) && Number.isFinite(total)) {

                totales[codigo] = total;
            }
        });

        const maximo = Math.max(
            1,
            ...Object.values(totales)
        );

        grafico.replaceChildren();

        estados.forEach(estado => {
            const total = totales[estado.codigo];

            const indicador = $(estado.elemento);
            if (indicador) {
                indicador.textContent =
                    total.toLocaleString("es-NI");
            }

            const fila = document.createElement("div");
            fila.className = "api-barra-fila";

            const nombre = document.createElement("span");
            nombre.className = "api-barra-etiqueta";
            nombre.textContent = estado.nombre;

            const pista = document.createElement("div");
            pista.className = "api-barra-pista";

            const barra = document.createElement("div");
            barra.className = "api-barra-valor";

            barra.style.width =
                (total / maximo * 100) + "%";

            pista.appendChild(barra);

            const valor = document.createElement("span");
            valor.className = "api-barra-numero";
            valor.textContent =
                total.toLocaleString("es-NI");

            fila.append(nombre, pista, valor);
            grafico.appendChild(fila);
        });
    }

    window.nautaEstadisticasCargando = function () {
        establecerCarga(true);
        mostrarMensaje("Consultando estadísticas...");
    };

    window.nautaEstadisticasDataReady = function (datos) {
        establecerCarga(false);

        if (!Array.isArray(datos)) {
            mostrarMensaje(
                "La API devolvió un formato inesperado.",
                true
            );
            return;
        }

        mostrarDatos(datos);

        mostrarMensaje(
            "Estadísticas actualizadas correctamente."
        );
    };

    window.nautaEstadisticasDataError = function (texto) {
        establecerCarga(false);

        mostrarMensaje(
            texto || "No se pudo consultar la API.",
            true
        );
    };

    botonConsultar.addEventListener("click", () => {
        try {
            puente.consultar();
        } catch (error) {
            establecerCarga(false);
            mostrarMensaje(
                "No se pudo iniciar la consulta de estadísticas.",
                true
            );
        }
    });
})();
