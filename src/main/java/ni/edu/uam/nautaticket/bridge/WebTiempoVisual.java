package ni.edu.uam.nautaticket.bridge;

/** Elementos de tiempo añadidos al dashboard WebView. */
public final class WebTiempoVisual {
    private WebTiempoVisual() { }

    public static final String RELOJ_TECNICO = """
            (function () {
                var asignados = [];
                function formato(total) {
                    total = Math.max(0, Math.floor(total));
                    var h = Math.floor(total / 3600);
                    var m = Math.floor(total % 3600 / 60);
                    var s = total % 60;
                    return (h ? h + ' h ' : '') + m + ' min ' + String(s).padStart(2, '0') + ' s';
                }
                function pintar() {
                    var titulo = document.getElementById('atencionSeleccionado');
                    if (!titulo) return;
                    var reloj = document.getElementById('nautaRelojTicket');
                    if (!reloj) {
                        reloj = document.createElement('p');
                        reloj.id = 'nautaRelojTicket';
                        reloj.style.cssText = 'font-size:16px;color:#c9baf4;font-weight:600';
                        titulo.insertAdjacentElement('afterend', reloj);
                    }
                    var seleccionado = /Ticket #(\\d+)/.exec(titulo.textContent || '');
                    var ticket = seleccionado && asignados.find(function (t) {
                        return t.id === Number(seleccionado[1]);
                    });
                    if (!ticket || !ticket.fechaAsignacion) {
                        reloj.textContent = 'Selecciona un ticket asignado para ver su tiempo.';
                        return;
                    }
                    var inicio = new Date(ticket.fechaAsignacion).getTime();
                    reloj.textContent = isNaN(inicio) ? 'Tiempo no disponible.'
                        : 'Tiempo transcurrido: ' + formato((Date.now() - inicio) / 1000);
                }
                var espera = setInterval(function () {
                    if (typeof window.nautaAtencionDataReady !== 'function') return;
                    clearInterval(espera);
                    var original = window.nautaAtencionDataReady;
                    window.nautaAtencionDataReady = function (datos) {
                        asignados = Array.isArray(datos.asignados) ? datos.asignados : [];
                        original(datos);
                        pintar();
                    };
                    setInterval(pintar, 1000);
                    pintar();
                }, 100);
            })();
            """;

    public static final String GRAFICOS_ADMIN = """
            (function () {
                var resumen = document.getElementById('resumen');
                if (!resumen || !window.nautaTiempoBridge) return;
                var panel = document.createElement('section');
                panel.style.cssText = 'margin-top:24px;padding:22px;background:#0d0d0d;border:1px solid #292929;border-radius:9px';
                var encabezado = document.createElement('h2');
                encabezado.textContent = 'Estados de tickets y tiempos de resolución';
                encabezado.style.cssText = 'color:#f2f2f4;font-size:18px';
                var ayuda = document.createElement('p');
                ayuda.textContent = 'Estado actual de los tickets y promedio de resolución por técnico.';
                ayuda.style.color = '#a5a5ad';
                var boton = document.createElement('button');
                boton.type = 'button';
                boton.className = 'button button-secondary';
                boton.textContent = 'Actualizar gráficos';
                var mensaje = document.createElement('p');
                mensaje.style.color = '#c9baf4';
                var estadosGrafico = document.createElement('div');
                var promedio = document.createElement('div');
                panel.append(encabezado, ayuda, boton, mensaje, estadosGrafico, promedio);
                resumen.appendChild(panel);
                function formato(total) {
                    total = Math.max(0, Math.round(total));
                    return Math.floor(total / 3600) + ' h '
                        + String(Math.floor(total % 3600 / 60)).padStart(2, '0') + ' min '
                        + String(total % 60).padStart(2, '0') + ' s';
                }
                function graficoEstados(contenedor, estados) {
                    contenedor.replaceChildren();
                    var h = document.createElement('h3');
                    h.textContent = 'Tickets por estado';
                    h.style.cssText = 'color:#f2f2f4;font-size:15px;margin:24px 0 14px';
                    contenedor.appendChild(h);
                    var opciones = [
                        { codigo: 'PENDIENTE', nombre: 'Pendiente', color: '#d9a441' },
                        { codigo: 'EN_PROCESO', nombre: 'En proceso', color: '#5d91d6' },
                        { codigo: 'RESUELTO', nombre: 'Resuelto', color: '#37b978' },
                        { codigo: 'CERRADO', nombre: 'Cerrado', color: '#8792a1' }
                    ];
                    var maximo = Math.max(1, ...opciones.map(function (e) {
                        return Number(estados[e.codigo]) || 0;
                    }));
                    opciones.forEach(function (estado) {
                        var total = Number(estados[estado.codigo]) || 0;
                        var fila = document.createElement('div');
                        fila.style.cssText = 'display:grid;grid-template-columns:minmax(130px,25%) 1fr 130px;gap:12px;align-items:center;margin:10px 0;color:#ddd;font-size:12px';
                        var nombre = document.createElement('span');
                        nombre.textContent = estado.nombre;
                        var pista = document.createElement('div');
                        pista.style.cssText = 'height:17px;background:#292929;border-radius:4px';
                        var barra = document.createElement('div');
                        barra.style.cssText = 'height:100%;background:' + estado.color
                            + ';border-radius:4px;width:' + total / maximo * 100 + '%';
                        pista.appendChild(barra);
                        var valor = document.createElement('span');
                        valor.textContent = String(total) + (total === 1 ? ' ticket' : ' tickets');
                        fila.append(nombre, pista, valor);
                        contenedor.appendChild(fila);
                    });
                }
                function grafico(contenedor, titulo, datos, campo) {
                    contenedor.replaceChildren();
                    var h = document.createElement('h3');
                    h.textContent = titulo;
                    h.style.cssText = 'color:#f2f2f4;font-size:15px;margin:24px 0 14px';
                    contenedor.appendChild(h);
                    if (!datos.length) {
                        var vacio = document.createElement('p');
                        vacio.textContent = 'Aún no hay tickets resueltos.';
                        vacio.style.color = '#a5a5ad';
                        contenedor.appendChild(vacio);
                    }
                    var maximo = Math.max(1, ...datos.map(function (d) { return Number(d[campo]) || 0; }));
                    var tiempos = datos.map(function (d) { return Number(d[campo]) || 0; });
                    var menorTiempo = tiempos.length ? Math.min(...tiempos) : 0;
                    var mayorTiempo = tiempos.length ? Math.max(...tiempos) : 0;
                    datos.forEach(function (d) {
                        var fila = document.createElement('div');
                        fila.style.cssText = 'display:grid;grid-template-columns:minmax(130px,25%) 1fr 130px;gap:12px;align-items:center;margin:10px 0;color:#ddd;font-size:12px';
                        var nombre = document.createElement('span');
                        nombre.textContent = d.nombre;
                        var pista = document.createElement('div');
                        pista.style.cssText = 'height:17px;background:#292929;border-radius:4px';
                        var barra = document.createElement('div');
                        var color = '#9783db';
                        if (menorTiempo < mayorTiempo) {
                            if (Number(d[campo]) === menorTiempo) color = '#37b978';
                            else if (Number(d[campo]) === mayorTiempo) color = '#df6674';
                        }
                        barra.style.cssText = 'height:100%;background:' + color + ';border-radius:4px;width:'
                            + Math.max(2, Number(d[campo]) / maximo * 100) + '%';
                        pista.appendChild(barra);
                        var valor = document.createElement('span');
                        valor.textContent = formato(d[campo]);
                        if (campo === 'promedio') valor.title = d.cantidad + ' tickets resueltos';
                        fila.append(nombre, pista, valor);
                        contenedor.appendChild(fila);
                    });
                }
                window.nautaTiemposDataReady = function (datos) {
                    boton.disabled = false;
                    mensaje.textContent = '';
                    graficoEstados(estadosGrafico, datos.estados || {});
                    grafico(promedio, 'Tiempo promedio de todos sus tickets',
                        Array.isArray(datos.tecnicos) ? datos.tecnicos : [], 'promedio');
                };
                window.nautaTiemposDataError = function (texto) {
                    boton.disabled = false;
                    mensaje.textContent = texto;
                };
                function consultar() {
                    boton.disabled = true;
                    mensaje.textContent = 'Consultando gráficos...';
                    window.nautaTiempoBridge.consultar();
                }
                boton.addEventListener('click', consultar);
                consultar();
            })();
            """;
}
