document.addEventListener("DOMContentLoaded", () => {
    const tabButtons = document.querySelectorAll(".tab-btn");
    const tabPanes = document.querySelectorAll(".tab-pane");

    tabButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            const target = btn.getAttribute("data-tab");
            tabButtons.forEach((b) => {
                const isSelected = b === btn;
                b.classList.toggle("active", isSelected);
                b.setAttribute("aria-selected", String(isSelected));
            });
            tabPanes.forEach((pane) => {
                pane.classList.toggle("active", pane.id === "pane-" + target);
            });
        });
    });

    const togglePwBtn = document.getElementById("togglePassword");
    const pwInput = document.getElementById("password");
    if (togglePwBtn && pwInput) {
        togglePwBtn.addEventListener("click", () => {
            const hidden = pwInput.type === "password";
            pwInput.type = hidden ? "text" : "password";
            togglePwBtn.textContent = hidden ? "Hide" : "Show";
            togglePwBtn.setAttribute("aria-label", hidden ? "Hide password" : "Show password");
        });
    }

    const joinCodeInput = document.getElementById("joinCode");
    if (joinCodeInput) {
        joinCodeInput.addEventListener("input", () => {
            const cleaned = joinCodeInput.value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 8);
            joinCodeInput.value = cleaned.length > 4
                ? cleaned.slice(0, 4) + "-" + cleaned.slice(4)
                : cleaned;
        });
    }
});
