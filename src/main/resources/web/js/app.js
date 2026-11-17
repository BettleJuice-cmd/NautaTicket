document.addEventListener("DOMContentLoaded", () => {

    const profilesView = document.getElementById("profilesView");
    const loginView = document.getElementById("loginView");
    const profileLabel = document.getElementById("profileLabel");

    const backBtn = document.getElementById("backBtn");
    const loginForm = document.getElementById("loginForm");

    const correo = document.getElementById("correo");
    const password = document.getElementById("password");
    const message = document.getElementById("message");

    const submitBtn = loginForm.querySelector(
        'button[type="submit"]'
    );

    let selectedProfile = null;
    let procesando = false;

    function mostrarMensaje(texto) {
        message.textContent = texto;
    }

    function establecerCarga(cargando) {
        procesando = cargando;

        submitBtn.disabled = cargando;
        backBtn.disabled = cargando;

        submitBtn.textContent = cargando
            ? "Verificando..."
            : "Ingresar";
    }

    function mostrarLogin(perfil, nombre) {
        selectedProfile = perfil;
        profileLabel.textContent = nombre;

        profilesView.hidden = true;
        loginView.hidden = false;

        loginForm.reset();
        mostrarMensaje("");

        correo.focus();
    }

    function volver() {
        if (procesando) {
            return;
        }

        selectedProfile = null;

        loginForm.reset();
        mostrarMensaje("");

        loginView.hidden = true;
        profilesView.hidden = false;
    }

    document.querySelectorAll("[data-profile]").forEach(button => {
        button.addEventListener("click", () => {
            mostrarLogin(
                button.dataset.profile,
                button.dataset.label
            );
        });
    });

    backBtn.addEventListener("click", volver);

    // Java puede invocar esta función para comunicar errores.
    window.nautaLoginError = function (texto) {
        establecerCarga(false);
        password.value = "";

        mostrarMensaje(
            texto || "No se pudo iniciar sesión."
        );
    };

    loginForm.addEventListener("submit", event => {

        event.preventDefault();

        if (procesando) {
            return;
        }

        if (!selectedProfile) {
            mostrarMensaje("Selecciona un perfil.");
            return;
        }

        const correoIngresado = correo.value.trim();

        if (!correoIngresado || !correo.checkValidity()) {
            mostrarMensaje(
                "Ingresa un correo electrónico válido."
            );
            return;
        }

        if (!password.value) {
            mostrarMensaje("Ingresa tu contraseña.");
            return;
        }

        const puente = window.nautaBridge;

        if (!puente) {
            password.value = "";

            mostrarMensaje(
                "No se detectó la conexión con JavaFX."
            );
            return;
        }

        establecerCarga(true);
        mostrarMensaje("");

        try {

            // Este método pertenece a WebLoginBridge.java.
            puente.iniciarSesion(
                correoIngresado,
                password.value,
                selectedProfile
            );

            password.value = "";

        } catch (error) {

            const detalle = String(error);

            // WebPreviewApp muestra este aviso
            // en la consola de IntelliJ mediante setOnAlert.
            window.alert(
                "Error al invocar WebLoginBridge: " + detalle
            );

            password.value = "";
            establecerCarga(false);

            mostrarMensaje(
                "No se pudo comunicar el formulario " +
                "con el servicio Java."
            );
        }
    });

});
