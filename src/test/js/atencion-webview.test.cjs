const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const script = fs.readFileSync(path.resolve(__dirname,
    '../../main/resources/web/js/atencion.js'), 'utf8');

class Elemento {
    constructor() {
        this.children = [];
        this.listeners = new Map();
        this.style = {};
        this.value = '';
        this.textContent = '';
        this.hidden = false;
        this.disabled = false;
        const clases = new Set();
        this.classList = {
            add: nombre => clases.add(nombre),
            remove: nombre => clases.delete(nombre),
            contains: nombre => clases.has(nombre),
            toggle: (nombre, activo) => activo ? clases.add(nombre) : clases.delete(nombre)
        };
    }

    appendChild(hijo) { this.children.push(hijo); return hijo; }
    append(...hijos) { this.children.push(...hijos); }
    prepend(hijo) { this.children.unshift(hijo); }
    replaceChildren() { this.children = []; }
    addEventListener(tipo, listener) { this.listeners.set(tipo, listener); }
    setAttribute(nombre, valor) { this[nombre] = valor; }
    removeAttribute(nombre) { delete this[nombre]; }
    querySelector() { return null; }
    focus() {}
    pulsar() { this.listeners.get('click')?.(); }
}

function preparar(visible = true) {
    const elementos = new Map();
    const obtener = id => {
        if (!elementos.has(id)) elementos.set(id, new Elemento());
        return elementos.get(id);
    };
    obtener('atencion').hidden = !visible;

    const eventos = new Map();
    const temporizadores = new Map();
    const llamadas = { consultas: 0, acciones: [] };
    let siguienteTemporizador = 1;
    const ventana = {
        nautaAtencionBridge: {
            listarDatos() { llamadas.consultas++; },
            ejecutarAccion(...args) { llamadas.acciones.push(args); }
        },
        addEventListener(tipo, listener) { eventos.set(tipo, listener); }
    };
    const documento = {
        head: new Elemento(),
        getElementById: obtener,
        createElement: () => new Elemento()
    };
    vm.runInNewContext(script, {
        window: ventana,
        document: documento,
        console,
        setTimeout(callback, demora) {
            assert.equal(demora, 5000);
            const id = siguienteTemporizador++;
            temporizadores.set(id, callback);
            return id;
        },
        clearTimeout(id) { temporizadores.delete(id); }
    });
    return {
        obtener, ventana, eventos, temporizadores, llamadas,
        navegar(visibilidad) {
            obtener('atencion').hidden = !visibilidad;
            eventos.get('nautaSeccionCambiada')?.();
        },
        avanzar() {
            assert.equal(temporizadores.size, 1);
            const [id, callback] = temporizadores.entries().next().value;
            temporizadores.delete(id);
            callback();
        }
    };
}

const datos = (activos = 1, asignados = [
    { id: 7, titulo: 'Propio', fecha: 'hoy', estado: 'EN_PROCESO' }
]) => ({
    pendientes: [{ id: 8, titulo: 'Nuevo', fecha: 'hoy', estado: 'PENDIENTE' }],
    asignados,
    ticketsActivos: activos,
    capacidadMaxima: 3
});

test('inicia al entrar, no duplica temporizadores y omite consultas ocupadas', () => {
    const app = preparar(false);
    assert.equal(app.llamadas.consultas, 0);
    assert.equal(app.temporizadores.size, 0);
    app.navegar(true);
    assert.equal(app.llamadas.consultas, 1);
    app.navegar(true);
    assert.equal(app.temporizadores.size, 1);
    assert.equal(app.llamadas.consultas, 1);
    app.ventana.nautaAtencionDataReady(datos());
    app.avanzar();
    assert.equal(app.llamadas.consultas, 2);
    app.avanzar();
    assert.equal(app.llamadas.consultas, 2);
    assert.equal(app.temporizadores.size, 1);
});

test('actualiza capacidad, selección y solución sin interrumpir operaciones', () => {
    const app = preparar();
    app.ventana.nautaAtencionDataReady(datos());
    assert.equal(app.obtener('atencionCapacidadValor').textContent, '1 / 3');
    app.obtener('atencionAsignadosBody').children[0].pulsar();
    app.obtener('atencionSolucion').value = 'Texto en curso';
    app.avanzar();
    assert.equal(app.obtener('atencionSolucion').value, 'Texto en curso');
    assert.equal(app.obtener('atencionSolucion').disabled, false);
    assert.equal(app.obtener('atencionResolver').disabled, true);
    app.ventana.nautaAtencionDataReady(datos(3));
    assert.equal(app.obtener('atencionCapacidadValor').textContent, '3 / 3');
    assert.equal(app.obtener('atencionPendientesBody').children[0]
        .children[0].textContent, '8');
    assert.equal(app.obtener('atencionAsignadosBody').children.length, 1);
    assert.equal(app.obtener('atencionTomar').disabled, true);
    assert.equal(app.obtener('atencionSolucion').value, 'Texto en curso');
    assert.equal(app.obtener('atencionAsignadosBody').children[0]
        .classList.contains('seleccionado'), true);

    app.obtener('atencionResolver').pulsar();
    assert.equal(app.llamadas.acciones.length, 1);
    app.avanzar();
    assert.equal(app.llamadas.consultas, 2);
    app.ventana.nautaAtencionActionResult({ correcto: false, mensaje: 'Error' });
    assert.equal(app.obtener('atencionSolucion').value, 'Texto en curso');
    assert.equal(app.llamadas.consultas, 3);
});

test('detiene el refresco al salir y descarta una respuesta anterior al regreso', () => {
    const app = preparar();
    assert.equal(app.llamadas.consultas, 1);
    app.navegar(false);
    assert.equal(app.temporizadores.size, 0);
    app.navegar(true);
    assert.equal(app.llamadas.consultas, 1);
    app.ventana.nautaAtencionDataReady(datos(3));
    assert.equal(app.llamadas.consultas, 2);
    assert.notEqual(app.obtener('atencionCapacidadValor').textContent, '3 / 3');
    app.ventana.nautaAtencionDataReady(datos(2));
    assert.equal(app.obtener('atencionCapacidadValor').textContent, '2 / 3');
    app.eventos.get('pagehide')();
    assert.equal(app.temporizadores.size, 0);
});

test('el dashboard abre la vista permitida y bloquea rutas de otros roles', () => {
    const dashboard = fs.readFileSync(path.resolve(__dirname,
        '../../main/resources/web/js/dashboard.js'), 'utf8');
    const html = fs.readFileSync(path.resolve(__dirname,
        '../../main/resources/web/dashboard.html'), 'utf8');
    const rolesHtml = Object.fromEntries(
        [...html.matchAll(/<button\b[^>]*data-section="([^"]+)"[^>]*>/g)]
            .map(([etiqueta, seccion]) => [seccion,
                etiqueta.match(/data-roles="([^"]+)"/)?.[1]])
    );
    assert.equal(rolesHtml.resumen, 'Administrador');
    assert.equal(rolesHtml.tickets, 'Administrador,Empleado');
    assert.equal(rolesHtml.atencion, 'Soporte técnico');
    const roles = {
        resumen: 'Administrador',
        tickets: 'Administrador,Empleado',
        atencion: 'Soporte técnico',
        historial: 'Administrador,Soporte técnico',
        reportes: 'Administrador,Soporte técnico',
        administracion: 'Administrador'
    };

    for (const [perfil, inicial, prohibida] of [
        ['Empleado', 'tickets', 'resumen'],
        ['Soporte técnico', 'atencion', 'tickets'],
        ['Administrador', 'resumen', 'atencion']
    ]) {
        const elementos = new Map();
        const obtener = id => {
            if (!elementos.has(id)) elementos.set(id, new Elemento());
            return elementos.get(id);
        };
        const secciones = Object.keys(roles).map(id => {
            const elemento = obtener(id);
            elemento.hidden = true;
            elemento.dataset = { roles: roles[id] };
            elemento.classList.add('workspace-section');
            return elemento;
        });
        const navegacion = Object.keys(roles).map(id => {
            const elemento = new Elemento();
            elemento.dataset = { section: id, roles: roles[id] };
            return elemento;
        });
        const eventos = [];
        let alCargar;
        const ventana = {
            nautaDashboardBridge: {
                getCorreo: () => 'usuario@prueba.invalid',
                getPerfil: () => perfil
            },
            dispatchEvent: evento => eventos.push(evento.detail.seccion)
        };
        const documento = {
            head: new Elemento(),
            body: new Elemento(),
            getElementById: obtener,
            createElement: () => new Elemento(),
            querySelector: () => new Elemento(),
            querySelectorAll(selector) {
                if (selector === '[data-roles]') return [...navegacion, ...secciones];
                if (selector === '.workspace-section') return secciones;
                if (selector === '[data-section]') return navegacion;
                return [];
            },
            addEventListener(tipo, listener) {
                if (tipo === 'DOMContentLoaded') alCargar = listener;
            }
        };
        vm.runInNewContext(dashboard, {
            window: ventana,
            document: documento,
            CustomEvent: class { constructor(tipo, opciones) { this.detail = opciones.detail; } }
        });
        alCargar();
        assert.equal(obtener(inicial).hidden, false, perfil);
        assert.equal(navegacion[Object.keys(roles).indexOf(prohibida)].hidden, true);
        navegacion[Object.keys(roles).indexOf(prohibida)].pulsar();
        assert.equal(obtener(inicial).hidden, false, perfil);
        assert.equal(obtener(prohibida).hidden, true, perfil);
        assert.equal(eventos.at(-1), inicial);
    }
});
