const endpoints = {
    info: "/api/demo/info",
    state: "/api/demo/state",
    plans: "/api/plans",
    subscriptions: "/api/demo/subscriptions",
    payments: "/api/demo/payments",
    events: "/api/demo/events",
    webhooks: "/api/demo/webhooks",
    startSubscription: "/api/subscriptions/start",

    simulateRecurringCharge: (subscriptionId) =>
        `/api/subscriptions/${encodeURIComponent(subscriptionId)}/simulate-recurring-charge`,

    changePlan: (subscriptionId) =>
        `/api/subscriptions/${encodeURIComponent(subscriptionId)}/change-plan`,

    cancelSubscription: (subscriptionId) =>
        `/api/subscriptions/${encodeURIComponent(subscriptionId)}/cancel`,
};

const elements = {
    refreshButton: document.querySelector("#refreshButton"),
    resetButton: document.querySelector("#resetButton"),
    messageBox: document.querySelector("#messageBox"),

    integrationGrid: document.querySelector("#integrationGrid"),
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

    recurringChargeForm: document.querySelector("#recurringChargeForm"),
    recurringSubscriptionSelect: document.querySelector("#recurringSubscriptionSelect"),
    recurringCardNumberInput: document.querySelector("#recurringCardNumberInput"),

    changePlanForm: document.querySelector("#changePlanForm"),
    changePlanSubscriptionSelect: document.querySelector("#changePlanSubscriptionSelect"),
    changePlanPlanSelect: document.querySelector("#changePlanPlanSelect"),

    cancelSubscriptionForm: document.querySelector("#cancelSubscriptionForm"),
    cancelSubscriptionSelect: document.querySelector("#cancelSubscriptionSelect"),

    receivedWebhooksCount: document.querySelector("#receivedWebhooksCount"),
    receivedWebhooksTable: document.querySelector("#receivedWebhooksTable"),
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
            info,
            state,
            plans,
            subscriptions,
            payments,
            events,
            webhooks,
        ] = await Promise.all([
            requestJson(endpoints.info),
            requestJson(endpoints.state),
            requestJson(endpoints.plans),
            requestJson(endpoints.subscriptions),
            requestJson(endpoints.payments),
            requestJson(endpoints.events),
            requestJson(endpoints.webhooks),
        ]);

        const safePlans = plans ?? [];
        const safeSubscriptions = subscriptions ?? [];

        renderState(state);
        renderIntegrationInfo(info);
        renderPlans(safePlans);
        renderActionPlanSelect(safePlans);
        renderActionSubscriptionSelects(safeSubscriptions);
        renderSubscriptions(safeSubscriptions);
        renderPayments(payments ?? []);
        renderReceivedWebhooks(webhooks ?? []);
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

function renderIntegrationInfo(info) {
    const items = [
        ["App", info?.name ?? "-"],
        ["Mock Payment Base URL", info?.mock_payment_base_url ?? "-"],
        ["Public Base URL", info?.public_base_url ?? "-"],
        ["Webhook Base URL", info?.webhook_base_url ?? "-"],
        ["Webhook Path", info?.webhook_path ?? "-"],
        ["Webhook URL", info?.webhook_url ?? "-"],
    ];

    elements.integrationGrid.innerHTML = items
        .map(([label, value]) => `
            <article class="integration-item">
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

function renderReceivedWebhooks(webhooks) {
    elements.receivedWebhooksCount.textContent = webhooks.length;

    if (!webhooks.length) {
        elements.receivedWebhooksTable.innerHTML = tableEmptyRow(8, "Todavía no hay webhooks recibidos por StreamBox.");
        return;
    }

    elements.receivedWebhooksTable.innerHTML = webhooks
        .slice()
        .reverse()
        .map((webhook) => {
            const eventLabel = webhook.action ?? "-";
            const eventType = webhook.type ?? "-";

            return `
                <tr>
                    <td class="mono">${escapeHtml(webhook.id)}</td>
                    <td class="mono">${escapeHtml(webhook.requestId ?? "-")}</td>
                    <td>
                        <div class="webhook-event">
                            <strong>${escapeHtml(eventLabel)}</strong>
                            <span>${escapeHtml(eventType)}</span>
                        </div>
                    </td>
                    <td class="mono">${escapeHtml(webhook.dataId ?? "-")}</td>
                    <td>${badge(String(webhook.validSignature))}</td>
                    <td>${badge(String(webhook.processed))}</td>
                    <td>
                        ${
                webhook.error
                    ? `<span class="webhook-error">${escapeHtml(webhook.error)}</span>`
                    : escapeHtml("-")
            }
                    </td>
                    <td>${escapeHtml(formatDate(webhook.receivedAt))}</td>
                </tr>
            `;
        })
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

async function simulateRecurringCharge(event) {
    event.preventDefault();

    const subscriptionId = elements.recurringSubscriptionSelect.value;

    if (!subscriptionId) {
        showMessage("Primero tenés que seleccionar una suscripción.", "error");
        return;
    }

    try {
        const response = await requestJson(endpoints.simulateRecurringCharge(subscriptionId), {
            method: "POST",
            body: JSON.stringify({
                card_number: elements.recurringCardNumberInput.value,
            }),
        });

        showMessage(
            `Cobro recurrente generado. Suscripción: ${response.subscription.status}. Pago: ${response.payment.status}.`
        );

        await loadDashboard({ silent: true });
    } catch (error) {
        console.error(error);
        showMessage(`No se pudo simular el cobro recurrente: ${error.message}`, "error");
    }
}

async function changeSubscriptionPlan(event) {
    event.preventDefault();

    const subscriptionId = elements.changePlanSubscriptionSelect.value;
    const planId = elements.changePlanPlanSelect.value;

    if (!subscriptionId) {
        showMessage("Primero tenés que seleccionar una suscripción.", "error");
        return;
    }

    if (!planId) {
        showMessage("Primero tenés que seleccionar un plan.", "error");
        return;
    }

    try {
        const response = await requestJson(endpoints.changePlan(subscriptionId), {
            method: "POST",
            body: JSON.stringify({
                plan_id: planId,
            }),
        });

        showMessage(
            `Plan cambiado. Suscripción ${response.subscription.id}: ${response.subscription.planName}.`
        );

        await loadDashboard({ silent: true });
    } catch (error) {
        console.error(error);
        showMessage(`No se pudo cambiar el plan: ${error.message}`, "error");
    }
}

async function cancelSubscription(event) {
    event.preventDefault();

    const subscriptionId = elements.cancelSubscriptionSelect.value;

    if (!subscriptionId) {
        showMessage("Primero tenés que seleccionar una suscripción.", "error");
        return;
    }

    const confirmed = window.confirm(
        `¿Seguro que querés cancelar la suscripción ${subscriptionId}?`
    );

    if (!confirmed) {
        return;
    }

    try {
        const response = await requestJson(endpoints.cancelSubscription(subscriptionId), {
            method: "POST",
        });

        showMessage(
            `Suscripción cancelada: ${response.subscription.id} · ${response.subscription.status}.`
        );

        await loadDashboard({ silent: true });
    } catch (error) {
        console.error(error);
        showMessage(`No se pudo cancelar la suscripción: ${error.message}`, "error");
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

function renderActionPlanSelect(plans) {
    const previousValue = elements.changePlanPlanSelect.value;

    if (!plans.length) {
        elements.changePlanPlanSelect.innerHTML = `<option value="">Sin planes disponibles</option>`;
        return;
    }

    elements.changePlanPlanSelect.innerHTML = plans
        .map((plan) => `
            <option value="${escapeHtml(plan.id)}">
                ${escapeHtml(plan.name)} · ${escapeHtml(formatMoney(plan.amount, plan.currency))} / mes
            </option>
        `)
        .join("");

    const exists = plans.some((plan) => plan.id === previousValue);

    if (exists) {
        elements.changePlanPlanSelect.value = previousValue;
    }
}

function renderActionSubscriptionSelects(subscriptions) {
    const selects = [
        elements.recurringSubscriptionSelect,
        elements.changePlanSubscriptionSelect,
        elements.cancelSubscriptionSelect,
    ];

    for (const select of selects) {
        const previousValue = select.value;

        if (!subscriptions.length) {
            select.innerHTML = `<option value="">Sin suscripciones</option>`;
            continue;
        }

        select.innerHTML = subscriptions
            .map((subscription) => `
                <option value="${escapeHtml(subscription.id)}">
                    ${escapeHtml(subscription.id)} · ${escapeHtml(subscription.planName)} · ${escapeHtml(subscription.status)}
                </option>
            `)
            .join("");

        const exists = subscriptions.some((subscription) => subscription.id === previousValue);

        if (exists) {
            select.value = previousValue;
        }
    }
}

elements.subscriptionForm.addEventListener("submit", startSubscription);
elements.recurringChargeForm.addEventListener("submit", simulateRecurringCharge);
elements.changePlanForm.addEventListener("submit", changeSubscriptionPlan);
elements.cancelSubscriptionForm.addEventListener("submit", cancelSubscription);

elements.refreshButton.addEventListener("click", () => loadDashboard());
elements.resetButton.addEventListener("click", resetState);

loadDashboard();