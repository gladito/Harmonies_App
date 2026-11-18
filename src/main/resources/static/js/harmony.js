document.addEventListener("change", function (event) {
  const select = event.target;
  if (!select || select.name !== "paymentMethod") return;
  const form = select.closest("form");
  if (!form) return;
  const transferBox = form.querySelector(".pay-transfer-fields");
  const cardBox = form.querySelector(".pay-card-fields");
  const isCard = select.value === "CARD";
  if (transferBox) {
    transferBox.style.display = isCard ? "none" : "block";
    transferBox.querySelectorAll("input").forEach((inp) => (inp.required = !isCard));
  }
  if (cardBox) {
    cardBox.style.display = isCard ? "block" : "none";
    cardBox.querySelectorAll("input").forEach((inp) => (inp.required = isCard));
  }
});

document.addEventListener("submit", function (event) {
  const form = event.target;
  if (!form || (form.method && form.method.toLowerCase() !== "post")) {
    return;
  }

  // Excluir únicamente el formulario de login para no bloquear la suite automatizada E2E
  const action = form.getAttribute("action") || "";
  if (action.endsWith("/login")) {
    return;
  }

  const submitter = event.submitter;
  let message =
    (submitter && submitter.dataset && submitter.dataset.confirm) ||
    (form.dataset && form.dataset.confirm);

  if (!message) {
    const label = submitter
      ? (submitter.textContent || submitter.value || "").trim().replace(/\s+/g, " ")
      : "";
    const isEnglish = (document.documentElement.lang || "").toLowerCase().startsWith("en");
    if (label) {
      message = isEnglish
        ? `Are you sure you want to confirm "${label}"?`
        : `¿Estás seguro de que querés confirmar "${label}"?`;
    } else {
      message = isEnglish
        ? "Are you sure you want to confirm this action?"
        : "¿Estás seguro de que querés confirmar esta acción?";
    }
  }

  if (!window.confirm(message)) {
    event.preventDefault();
  }
});
