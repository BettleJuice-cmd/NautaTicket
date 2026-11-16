document.addEventListener("DOMContentLoaded", () => {

    const sections = document.querySelectorAll(".workspace-section");
    const navButtons = document.querySelectorAll(".nav-item");
    const currentSection = document.getElementById("currentSection");

    const names = {
        resumen: "Resumen",
        tickets: "Tickets",
        atencion: "Atención",
        historial: "Historial",
        reportes: "Reportes"
    };

    function showSection(sectionId) {

        if (!names[sectionId]) {
            return;
        }

        sections.forEach(section => {
            section.hidden = section.id !== sectionId;
        });

        navButtons.forEach(button => {
            const active = button.dataset.section === sectionId;

            button.classList.toggle("active", active);

            if (active) {
                button.setAttribute("aria-current", "page");
            } else {
                button.removeAttribute("aria-current");
            }
        });

        currentSection.textContent = names[sectionId];
        document.title = "NautaTicket | " + names[sectionId];
    }

    navButtons.forEach(button => {
        button.addEventListener("click", () => {
            showSection(button.dataset.section);
        });
    });

    document.querySelectorAll("[data-go]").forEach(button => {
        button.addEventListener("click", () => {
            showSection(button.dataset.go);
        });
    });

    showSection("resumen");
});
