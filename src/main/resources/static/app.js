const endpoints = {
    state: "/api/demo/state",
    plans: "/api/plans",
    subscriptions: "/api/demo/subscriptions",
    payments: "/api/demo/payments",
    events: "/api/demo/events",
    startSubscription: "/api/subscriptions/start",
};

const elements = {
    refreshButton: document.querySelector("#refreshButton"),
    resetButton: document.querySelector("#resetButton"),
    messageBox: document.querySelector("#messageBox"),

    stateGrid: document.querySelector("#stateGrid"),
    plansGrid: document.querySelector("#plansGrid"),

    plansCount: document.querySelector("#plansCount"),
    subscriptionsCount: document.querySelector("#subscriptionsCount"),
    paymentsCount: document.querySelector("#paymentsCount"),
    eventsCount: document.querySelector("#eventsCount"),

    subscriptionsTable: document.querySelector("#subscriptionsTable"),
    paymentsTable: document.querySelector("#paymentsTable"),
    eventsTable: document.querySelector("#eventsTable"),

    subscriptionForm: document.querySelector("#subscriptionForm"),
    planSelect: document.querySelector("#planSelect"),
    payerEmailInput: document.querySelector("#payerEmailInput"),
    cardNumberInput: document.querySelector("#cardNumberInput"),
};

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatDate(value) {
    if (!value) {
        return "-";
    }

    try {
        return new Intl.DateTimeFormat("es-AR", {
            dateStyle: "short",
            timeStyle: "medium",
        }).format(new Date(value));
    } catch {
        return value;
    }
}

function formatMoney(amount, currency) {
    if (amount === null || amount === undefined) {
        return "-";
    }

    return `${Number(amount).toLocaleString("es-AR")} ${currency ?? ""}`.trim();
}

function statusClass(status) {
    const normalized = String(status ?? "").toLowerCase();

    if (
        normalized.includes("active") ||
        normalized.includes("approved")
    ) {
        return "status-success";
    }

    if (
        normalized.includes("pending") ||
        normalized.includes("process") ||
        normalized.includes("paused")
    ) {
        return "status-warning";
    }

    if (
        normalized.includes("failed") ||
        normalized.includes("rejected") ||
        normalized.includes("cancelled")
    ) {
        return "status-danger";
    }

    return "status-muted";
}

function badge(value) {
    return `<span class="status ${statusClass(value)}">${escapeHtml(value)}</span>`;
}

function showMessage(message, type = "info") {
    elements.messageBox.textContent = message;
    elements.messageBox.classList.remove("hidden", "message-error");

    if (type === "error") {
        elements.messageBox.classList.add("message-error");
    }

    window.clearTimeout(showMessage.timeoutId);
    showMessage.timeoutId = window.setTimeout(() => {
        elements.messageBox.classList.add("hidden");
    }, 5000);
}

async function requestJson(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            "Accept": "application/json",
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...(options.headers ?? {}),
        },
        ...options,
    });

    const text = await response.text();

    if (!response.ok) {
        throw new Error(text || `HTTP ${response.status}`);
    }

    if (!text) {
        return null;
    }

    return JSON.parse(text);
}

async function loadDashboard(options = {}) {
    setLoading(true);

    try {
        const [
            state,
            plans,
            subscriptions,
            payments,
            events,
        ] = await Promise.all([
            requestJson(endpoints.state),
            requestJson(endpoints.plans),
            requestJson(endpoints.subscriptions),
            requestJson(endpoints.payments),
            requestJson(endpoints.events),
        ]);

        renderState(state);
        renderPlans(plans ?? []);
        renderSubscriptions(subscriptions ?? []);
        renderPayments(payments ?? []);
        renderEvents(events ?? []);

        if (!options.silent) {
            showMessage("StreamBox Demo actualizado correctamente.");
        }
    } catch (error) {
        console.error(error);
        showMessage(`No se pudo cargar la demo: ${error.message}`, "error");
    } finally {
        setLoading(false);
    }
}

function setLoading(loading) {
    elements.refreshButton.disabled = loading;
    elements.resetButton.disabled = loading;
    elements.refreshButton.textContent = loading ? "Cargando..." : "Refrescar";
}

function renderState(state) {
    const items = [
        ["Suscripciones", state?.subscriptions_count ?? 0],
        ["Pagos", state?.payments_count ?? 0],
        ["Eventos", state?.events_count ?? 0],
        ["Storage", state?.storage_type ?? "-"],
        ["Archivo", state?.state_file_path ?? "-"],
    ];

    elements.stateGrid.innerHTML = items
        .map(([label, value]) => `
            <article class="metric">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(value)}</strong>
            </article>
        `)
        .join("");
}

function renderPlans(plans) {
    elements.plansCount.textContent = plans.length;

    renderPlanSelect(plans);

    if (!plans.length) {
        elements.plansGrid.innerHTML = `<div class="empty">No hay planes disponibles.</div>`;
        return;
    }

    elements.plansGrid.innerHTML = plans
        .map((plan) => `
            <article class="plan-card">
                <h3>${escapeHtml(plan.name)}</h3>
                <p>${escapeHtml(plan.description)}</p>
                <span class="plan-price">${escapeHtml(formatMoney(plan.amount, plan.currency))} / mes</span>
                <p class="mono">plan_id: ${escapeHtml(plan.id)}</p>
            </article>
        `)
        .join("");
}

function renderSubscriptions(subscriptions) {
    elements.subscriptionsCount.textContent = subscriptions.length;

    if (!subscriptions.length) {
        elements.subscriptionsTable.innerHTML = tableEmptyRow(6, "Todavía no hay suscripciones internas.");
        return;
    }

    elements.subscriptionsTable.innerHTML = subscriptions
        .map((subscription) => `
            <tr>
                <td class="mono">${escapeHtml(subscription.id)}</td>
                <td>
                    <strong>${escapeHtml(subscription.planName)}</strong>
                    <br />
                    <span class="mono">${escapeHtml(subscription.planId)}</span>
                </td>
                <td>${escapeHtml(subscription.payerEmail)}</td>
                <td class="mono">${escapeHtml(subscription.providerSubscriptionId ?? "-")}</td>
                <td>${badge(subscription.status)}</td>
                <td>${escapeHtml(formatDate(subscription.updatedAt))}</td>
            </tr>
        `)
        .join("");
}

function renderPayments(payments) {
    elements.paymentsCount.textContent = payments.length;

    if (!payments.length) {
        elements.paymentsTable.innerHTML = tableEmptyRow(6, "Todavía no hay pagos internos.");
        return;
    }

    elements.paymentsTable.innerHTML = payments
        .map((payment) => `
            <tr>
                <td class="mono">${escapeHtml(payment.id)}</td>
                <td class="mono">${escapeHtml(payment.subscriptionId)}</td>
                <td class="mono">${escapeHtml(payment.providerPaymentId)}</td>
                <td>${badge(payment.status)}</td>
                <td>${escapeHtml(formatMoney(payment.amount, payment.currency))}</td>
                <td>${escapeHtml(formatDate(payment.createdAt))}</td>
            </tr>
        `)
        .join("");
}

function renderEvents(events) {
    elements.eventsCount.textContent = events.length;

    if (!events.length) {
        elements.eventsTable.innerHTML = tableEmptyRow(4, "Todavía no hay eventos internos.");
        return;
    }

    elements.eventsTable.innerHTML = events
        .slice()
        .reverse()
        .map((event) => `
            <tr>
                <td class="mono">${escapeHtml(event.id)}</td>
                <td>${badge(event.type)}</td>
                <td>${escapeHtml(event.message)}</td>
                <td>${escapeHtml(formatDate(event.createdAt))}</td>
            </tr>
        `)
        .join("");
}

function tableEmptyRow(columns, message) {
    return `
        <tr>
            <td colspan="${columns}" class="empty">${escapeHtml(message)}</td>
        </tr>
    `;
}

async function startSubscription(event) {
    event.preventDefault();

    const request = {
        plan_id: elements.planSelect.value,
        payer_email: elements.payerEmailInput.value.trim(),
        card_number: elements.cardNumberInput.value,
    };

    try {
        const response = await requestJson(endpoints.startSubscription, {
            method: "POST",
            body: JSON.stringify(request),
        });

        showMessage(
            `Suscripción ${response.subscription.id} creada con estado ${response.subscription.status}. Pago: ${response.payment.status}.`
        );

        await loadDashboard({ silent: true });
    } catch (error) {
        console.error(error);
        showMessage(`No se pudo iniciar la suscripción: ${error.message}`, "error");
    }
}

async function resetState() {
    const confirmed = window.confirm(
        "¿Seguro que querés resetear el estado interno de StreamBox Demo?"
    );

    if (!confirmed) {
        return;
    }

    setLoading(true);

    try {
        await requestJson(endpoints.state, {
            method: "DELETE",
        });

        showMessage("Estado reseteado correctamente.");
        await loadDashboard({ silent: true });
    } catch (error) {
        console.error(error);
        showMessage(`No se pudo resetear el estado: ${error.message}`, "error");
    } finally {
        setLoading(false);
    }
}

function renderPlanSelect(plans) {
    const previousValue = elements.planSelect.value;

    if (!plans.length) {
        elements.planSelect.innerHTML = `<option value="">Sin planes disponibles</option>`;
        return;
    }

    elements.planSelect.innerHTML = plans
        .map((plan) => `
            <option value="${escapeHtml(plan.id)}">
                ${escapeHtml(plan.name)} · ${escapeHtml(formatMoney(plan.amount, plan.currency))} / mes
            </option>
        `)
        .join("");

    const exists = plans.some((plan) => plan.id === previousValue);

    if (exists) {
        elements.planSelect.value = previousValue;
    }
}

elements.subscriptionForm.addEventListener("submit", startSubscription);
elements.refreshButton.addEventListener("click", () => loadDashboard());
elements.resetButton.addEventListener("click", resetState);

loadDashboard();