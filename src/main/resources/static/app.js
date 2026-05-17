const endpoints = {
    info: "/api/demo/info",
    state: "/api/demo/state",
    stateStream: "/api/demo/state/stream",
    plans: "/api/plans",
    subscriptions: "/api/demo/subscriptions",
    payments: "/api/demo/payments",
    events: "/api/demo/events",
    webhooks: "/api/demo/webhooks",
    createSubscription: "/api/subscriptions",

    reconcileSubscription: (subscriptionId) =>
        `/api/subscriptions/${encodeURIComponent(subscriptionId)}/reconcile-provider`,

    paySubscription: (subscriptionId) =>
        `/api/subscriptions/${encodeURIComponent(subscriptionId)}/pay`,

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
    helpButton: document.querySelector("#helpButton"),
    helpModalBackdrop: document.querySelector("#helpModalBackdrop"),
    helpCloseButton: document.querySelector("#helpCloseButton"),
    helpNav: document.querySelector("#helpNav"),
    helpContent: document.querySelector("#helpContent"),
    webhookModalBackdrop: document.querySelector("#webhookModalBackdrop"),
    webhookCloseButton: document.querySelector("#webhookCloseButton"),
    webhookModalTitle: document.querySelector("#webhookModalTitle"),
    webhookModalSubtitle: document.querySelector("#webhookModalSubtitle"),
    webhookPayloadBox: document.querySelector("#webhookPayloadBox"),
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

    paymentForm: document.querySelector("#paymentForm"),
    paymentSubscriptionSelect: document.querySelector("#paymentSubscriptionSelect"),
    paymentCardNumberInput: document.querySelector("#paymentCardNumberInput"),

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

let receivedWebhooksById = new Map();
let autoRefreshTimeoutId;

const helpDocs = [
    {
        id: "what-is-streambox",
        title: "Qué es StreamBox Demo",
        description: "Qué representa esta aplicación dentro del ecosistema de pagos.",
        html: `
            <h3>Qué es StreamBox Demo</h3>

            <p>
                StreamBox Demo es una aplicación principal de ejemplo. Simula una plataforma de streaming
                donde un usuario puede elegir un plan, pagar con una tarjeta ficticia y quedar suscripto.
            </p>

            <p>
                Su objetivo no es ser una aplicación real de streaming, sino mostrar cómo una aplicación
                de negocio debería integrarse con un servicio de pagos.
            </p>

            <h4>Qué representa cada proyecto</h4>

            <ul>
                <li><strong>StreamBox Demo:</strong> representa la aplicación principal.</li>
                <li><strong>Mock Payment Service:</strong> representa la pasarela de pago simulada.</li>
                <li><strong>Mock Payment Studio:</strong> permite inspeccionar pagos, suscripciones y webhooks desde el lado del proveedor.</li>
            </ul>

            <div class="help-callout">
                Esta app sirve para aprender el flujo completo antes de integrar pagos en un proyecto real.
            </div>
        `,
    },
    {
        id: "subscription-flow",
        title: "Flujo completo",
        description: "Cómo se crea una suscripción y cómo se paga después.",
        html: `
        <h3>Flujo completo de suscripción y pago</h3>

        <p>
            El flujo principal está dividido en dos pasos para que se entienda mejor qué hace
            la aplicación principal y qué hace la pasarela simulada.
        </p>

        <h4>1. Suscribirme</h4>

        <ol>
            <li>El usuario elige un plan en StreamBox.</li>
            <li>StreamBox crea una suscripción interna en estado <code>CREATING</code>.</li>
            <li>StreamBox llama a <code>POST /preapproval</code> en Mock Payment Service.</li>
            <li>Mock Payment Service devuelve un <code>provider_subscription_id</code>.</li>
            <li>StreamBox guarda ese ID externo y pasa la suscripción interna a <code>PENDING</code>.</li>
            <li>La suscripción queda lista para pagar.</li>
        </ol>

        <h4>2. Pagar</h4>

        <ol>
            <li>El usuario selecciona una suscripción existente.</li>
            <li>El usuario elige una tarjeta ficticia.</li>
            <li>StreamBox llama a Mock Payment Service para simular el pago.</li>
            <li>Mock Payment Service genera un pago.</li>
            <li>Mock Payment Service envía un webhook firmado a StreamBox.</li>
            <li>StreamBox recibe el webhook, valida la firma y consulta el recurso actualizado.</li>
            <li>StreamBox actualiza su suscripción interna y registra el pago interno.</li>
        </ol>

        <pre><code>Suscribirme
  ↓
StreamBox crea suscripción interna CREATING
  ↓
Mock Payment Service crea preapproval
  ↓
StreamBox guarda providerSubscriptionId y pasa a PENDING

Pagar
  ↓
StreamBox solicita pago con tarjeta ficticia
  ↓
Mock Payment Service genera payment
  ↓
Mock Payment Service envía webhook
  ↓
StreamBox valida firma
  ↓
StreamBox consulta pago/preapproval
  ↓
StreamBox actualiza estado interno</code></pre>

        <div class="help-callout">
            El punto más importante es que crear la suscripción y pagarla son dos acciones separadas.
            Esto hace que la demo se parezca más a una integración real.
        </div>
    `,
    },
    {
        id: "fake-cards",
        title: "Tarjetas ficticias",
        description: "Qué tarjeta usar para simular cada resultado.",
        html: `
            <h3>Pagos con tarjetas ficticias</h3>

            <p>
                Las tarjetas ficticias permiten forzar distintos resultados sin usar tarjetas reales.
            </p>
            
            <p>
                La tarjeta se elige en el formulario <strong>Pagar</strong>,
                después de haber creado una suscripción.
            </p>

            <ul>
                <li><code>4111111111111111</code> → pago aprobado.</li>
                <li><code>4000000000000002</code> → pago rechazado por falta de fondos.</li>
                <li><code>4000000000000341</code> → pago pendiente.</li>
                <li><code>4000000000009995</code> → número de tarjeta inválido.</li>
                <li><code>4000000000000069</code> → tarjeta vencida.</li>
                <li><code>4000000000000259</code> → error de procesamiento.</li>
            </ul>

            <h4>Estados internos típicos</h4>

            <pre><code>Pago aprobado:
subscription.status = ACTIVE
payment.status = APPROVED

Pago rechazado:
subscription.status = PAYMENT_FAILED
payment.status = REJECTED

Pago pendiente:
subscription.status = PENDING
payment.status = IN_PROCESS</code></pre>

            <div class="help-callout help-warning">
                Estas tarjetas no son reales. Solo sirven para simular resultados dentro de Mock Payment Service.
            </div>
        `,
    },
    {
        id: "webhooks",
        title: "Webhooks recibidos",
        description: "Cómo StreamBox recibe, valida y procesa webhooks.",
        html: `
            <h3>Webhooks recibidos por StreamBox</h3>

            <p>
                Cuando Mock Payment Service genera un pago o cambia una suscripción, puede enviar un webhook
                a StreamBox.
            </p>

            <p>
                StreamBox recibe ese webhook en:
            </p>

            <pre><code>POST /api/webhooks/mock-payment</code></pre>

            <h4>Qué hace StreamBox al recibir un webhook</h4>

            <ol>
                <li>Lee los headers <code>x-request-id</code> y <code>x-signature</code>.</li>
                <li>Lee el body del webhook.</li>
                <li>Valida la firma HMAC SHA-256.</li>
                <li>Registra el webhook recibido.</li>
                <li>Consulta el pago o la suscripción en Mock Payment Service.</li>
                <li>Actualiza su estado interno.</li>
                <li>Marca el webhook como procesado.</li>
            </ol>

            <h4>Cómo leer la tabla</h4>

            <ul>
                <li><strong>Firma válida:</strong> indica si la firma del webhook fue correcta.</li>
                <li><strong>Procesado:</strong> indica si StreamBox pudo aplicar la lógica interna.</li>
                <li><strong>Error:</strong> muestra el motivo si algo falló.</li>
                <li><strong>Request ID:</strong> permite relacionar este webhook con la entrega saliente del mock.</li>
            </ul>

            <div class="help-callout">
                En Mock Payment Studio el webhook puede verse como <code>Entregado = Sí</code> y <code>Recibido interno = No</code>.
                Eso es correcto cuando el webhook fue entregado a StreamBox y no al receptor interno del mock.
            </div>
        `,
    },
    {
        id: "docker",
        title: "Configuración Docker",
        description: "Qué URLs usa la app y cómo evitar conflictos de puertos.",
        html: `
            <h3>Configuración Docker</h3>

            <p>
                StreamBox puede correr en un contenedor separado de Mock Payment Service.
                Para que ambos se comuniquen, deben compartir una red Docker externa.
            </p>

            <pre><code>docker network create payment-demo-network</code></pre>

            <h4>URLs importantes</h4>

            <ul>
                <li><strong>Mock Payment Base URL:</strong> URL que StreamBox usa para llamar al mock.</li>
                <li><strong>Public Base URL:</strong> URL que abre el navegador para entrar a StreamBox.</li>
                <li><strong>Webhook Base URL:</strong> URL que Mock Payment usa para enviar webhooks a StreamBox.</li>
            </ul>

            <h4>Ejemplo con contenedores separados</h4>

            <pre><code>MOCK_PAYMENT_BASE_URL=http://mock-payment-service:9090
DEMO_APP_PUBLIC_BASE_URL=http://localhost:8080
DEMO_APP_WEBHOOK_BASE_URL=http://subscription-demo-app:8080</code></pre>

            <h4>Si publicás StreamBox en otro puerto</h4>

            <p>
                Podés publicar StreamBox en otro puerto de tu máquina sin romper la comunicación entre contenedores.
            </p>

            <pre><code>DEMO_APP_HOST_PORT=8085 docker compose up --build</code></pre>

            <p>
                En ese caso:
            </p>

            <pre><code>Navegador:
http://localhost:8085

Mock Payment → StreamBox:
http://subscription-demo-app:8080/api/webhooks/mock-payment</code></pre>

            <div class="help-callout">
                El puerto publicado sirve para tu navegador. El puerto interno sirve para la comunicación entre contenedores.
            </div>
        `,
    },
    {
        id: "dashboard",
        title: "Cómo leer el dashboard",
        description: "Qué significa cada sección visible en StreamBox.",
        html: `
        <h3>Cómo leer el dashboard</h3>

        <h4>Estado de la aplicación principal</h4>

        <p>
            Muestra cuántas suscripciones, pagos, eventos y webhooks recibió StreamBox.
        </p>

        <h4>Configuración de integración</h4>

        <p>
            Muestra las URLs reales usadas para hablar con Mock Payment Service y para recibir webhooks.
        </p>

        <h4>Suscripción y pago</h4>

        <p>
            Esta sección está dividida en dos acciones.
        </p>

        <ul>
            <li><strong>Suscribirme:</strong> crea la suscripción interna y la preapproval en Mock Payment Service.</li>
            <li><strong>Pagar:</strong> genera el pago inicial de una suscripción existente usando una tarjeta ficticia.</li>
        </ul>

        <h4>Acciones sobre una suscripción</h4>

        <p>
            Permite simular cobros recurrentes, cambiar de plan o cancelar una suscripción.
        </p>

        <h4>Suscripciones internas</h4>

        <p>
            Muestra el estado que guarda StreamBox, no solo el estado del proveedor.
        </p>

        <p>
            Si la creación en el proveedor queda sin confirmar, la suscripción aparece como
            <code>RECONCILIATION_NEEDED</code> y puede reconciliarse por su referencia externa.
        </p>

        <h4>Pagos internos</h4>

        <p>
            Muestra los pagos que StreamBox registró después de consultar al proveedor.
        </p>

        <h4>Webhooks recibidos por StreamBox</h4>

        <p>
            Muestra los webhooks que llegaron a la aplicación principal, si la firma fue válida y si fueron procesados.
        </p>

        <h4>Eventos internos</h4>

        <p>
            Es un log educativo que permite seguir el flujo paso a paso.
        </p>
    `,
    },
    {
        id: "code-to-emulate",
        title: "Código para emular",
        description: "Qué partes de este proyecto sirven como referencia para un proyecto real.",
        html: `
        <h3>Qué código conviene emular en un proyecto real</h3>

        <p>
            StreamBox Demo tiene código educativo y código que representa patrones útiles para una aplicación real.
            La idea no es copiar todo tal cual, sino identificar qué partes muestran una arquitectura razonable
            para integrar pagos.
        </p>

        <h4>Código que sí conviene mirar</h4>

        <ul>
            <li><strong>MockPaymentClient:</strong> muestra cómo aislar las llamadas HTTP al proveedor.</li>
            <li><strong>SubscriptionService:</strong> muestra cómo orquestar suscripción interna, proveedor, pagos y webhooks.</li>
            <li><strong>MockPaymentWebhookController:</strong> muestra cómo recibir webhooks sin meter toda la lógica en el controller.</li>
            <li><strong>WebhookSignatureVerifier:</strong> muestra cómo separar la validación de firma.</li>
            <li><strong>DemoSubscription:</strong> muestra la idea de guardar una suscripción interna con un ID externo del proveedor.</li>
            <li><strong>DemoPayment:</strong> muestra la idea de registrar pagos internos asociados a la suscripción.</li>
            <li><strong>DemoReceivedWebhook:</strong> muestra cómo auditar webhooks recibidos, procesados y fallidos.</li>
            <li><strong>Mapeo de estados:</strong> muestra cómo convertir estados externos en estados propios de la app.</li>
        </ul>

        <h4>Código que no conviene copiar tal cual</h4>

        <ul>
            <li><strong>DemoStore:</strong> sirve para la demo, pero en producción debería reemplazarse por base de datos y repositorios.</li>
            <li><strong>PlanCatalogService hardcodeado:</strong> en producción los planes deberían venir de base de datos o configuración administrable.</li>
            <li><strong>Tarjetas ficticias:</strong> solo existen porque usamos un proveedor mock.</li>
            <li><strong>UI estática:</strong> sirve para aprender, pero no es el patrón obligatorio de una app real.</li>
            <li><strong>Endpoints de simulación:</strong> sirven para pruebas locales, no para producción.</li>
        </ul>

        <h4>Patrón principal a emular</h4>

        <pre><code>Crear suscripción interna
  ↓
Llamar al proveedor de pagos
  ↓
Guardar providerSubscriptionId
  ↓
Recibir webhook firmado
  ↓
Validar firma
  ↓
Consultar recurso actualizado al proveedor
  ↓
Actualizar estado interno
  ↓
Registrar pago, evento o error</code></pre>

        <h4>En un proyecto real</h4>

        <p>
            En una aplicación productiva, estas clases deberían adaptarse a entidades, repositorios y servicios reales.
        </p>

        <pre><code>BusinessSubscription
BusinessPayment
PaymentWebhookEvent
PaymentProviderClient
BillingService
WebhookSignatureVerifier
PaymentWebhookController</code></pre>

        <div class="help-callout">
            La enseñanza más importante de StreamBox no es cómo simular pagos, sino cómo una aplicación principal
            debería guardar estado propio, validar webhooks, consultar al proveedor y mapear estados externos a internos.
        </div>

        <p>
            Para más detalle, revisar el archivo:
        </p>

        <pre><code>docs/code-to-emulate-in-real-project.md</code></pre>
    `,
    },
    {
        id: "streambox-vs-studio",
        title: "StreamBox vs Studio",
        description: "Diferencia entre la app principal y el panel del mock.",
        html: `
            <h3>Diferencia entre StreamBox y Mock Payment Studio</h3>

            <p>
                StreamBox y Mock Payment Studio muestran el mismo flujo desde dos lados distintos.
            </p>

            <h4>StreamBox Demo</h4>

            <ul>
                <li>Representa la aplicación principal.</li>
                <li>Crea suscripciones desde el punto de vista del negocio.</li>
                <li>Guarda estado interno.</li>
                <li>Recibe webhooks.</li>
                <li>Valida firmas.</li>
                <li>Actualiza suscripciones y pagos internos.</li>
            </ul>

            <h4>Mock Payment Studio</h4>

            <ul>
                <li>Representa el lado de la pasarela simulada.</li>
                <li>Muestra suscripciones y pagos del proveedor.</li>
                <li>Muestra webhooks salientes.</li>
                <li>Permite reintentar entregas.</li>
                <li>Muestra trazabilidad y comparación de payloads.</li>
            </ul>

            <div class="help-callout">
                Para depurar bien una integración, conviene mirar ambos: StreamBox para ver cómo reaccionó la app principal y Mock Payment Studio para ver qué emitió el proveedor.
            </div>
        `,
    },
];

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
        normalized.includes("approved") ||
        normalized === "sí"
    ) {
        return "status-success";
    }

    if (
        normalized.includes("pending") ||
        normalized.includes("process") ||
        normalized.includes("paused") ||
        normalized.includes("creating") ||
        normalized.includes("reconciliation")
    ) {
        return "status-warning";
    }

    if (
        normalized.includes("failed") ||
        normalized.includes("rejected") ||
        normalized.includes("cancelled") ||
        normalized === "no"
    ) {
        return "status-danger";
    }

    return "status-muted";
}

function badge(value) {
    return `<span class="status ${statusClass(value)}">${escapeHtml(value)}</span>`;
}

function formatBoolean(value) {
    return value ? "Sí" : "No";
}

function formatJsonBlock(value) {
    if (!value) {
        return "No hay payload disponible para este webhook.";
    }

    try {
        return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
        return value;
    }
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
        throw buildRequestError(response, text);
    }

    if (!text) {
        return null;
    }

    return JSON.parse(text);
}

function buildRequestError(response, text) {
    const error = new Error(formatRequestError(response, text));
    error.status = response.status;
    error.rawBody = text;
    return error;
}

function formatRequestError(response, text) {
    const payload = parseJson(text);
    const details = extractErrorDetails(payload, text);
    const friendlyDetails = details
        .map(humanizeErrorDetail)
        .filter(Boolean);

    if (friendlyDetails.length) {
        return friendlyDetails.join(" ");
    }

    if (response.status === 404) {
        return "No se encontró el recurso solicitado.";
    }

    if (response.status === 400) {
        return "La solicitud tiene datos inválidos o incompletos.";
    }

    if (response.status >= 500) {
        return "Ocurrió un error interno. Revisá que las dos aplicaciones estén levantadas y configuradas.";
    }

    return `La operación falló con estado HTTP ${response.status}.`;
}

function parseJson(value) {
    if (!value) {
        return null;
    }

    try {
        return JSON.parse(value);
    } catch {
        return null;
    }
}

function extractErrorDetails(payload, text) {
    if (payload) {
        if (Array.isArray(payload.details) && payload.details.length) {
            return payload.details;
        }

        if (payload.message) {
            return [payload.message];
        }

        if (payload.error && !isGenericErrorLabel(payload.error)) {
            return [payload.error];
        }
    }

    return text ? [text] : [];
}

function isGenericErrorLabel(label) {
    return [
        "Bad Request",
        "Internal Server Error",
        "Not Found",
        "Solicitud inválida",
        "Error interno",
        "No encontrado",
    ].includes(label);
}

function humanizeErrorDetail(detail) {
    const text = stripJavaExceptionPrefix(String(detail ?? "").trim());

    if (!text) {
        return "";
    }

    if (text.startsWith("<")) {
        return "";
    }

    const embeddedPayload = parseEmbeddedJson(text);

    if (embeddedPayload) {
        return extractErrorDetails(embeddedPayload, "")
            .map(humanizeErrorDetail)
            .filter(Boolean)
            .join(" ");
    }

    const normalized = text.toLowerCase();

    if (
        normalized.includes("connection refused") ||
        normalized.includes("i/o error") ||
        normalized.includes("connect timed out") ||
        normalized.includes("read timed out")
    ) {
        return "No se pudo conectar con Mock Payment Service. Verificá que esté levantado y que el puerto configurado sea correcto.";
    }

    if (normalized.includes("simulated response failure after creating preapproval")) {
        return "Mock Payment Service creó la suscripción, pero simuló una pérdida de respuesta. Usá Reconciliar para confirmar el estado.";
    }

    if (normalized.includes("preapproval not found")) {
        return "Mock Payment Service no encontró la suscripción solicitada.";
    }

    if (normalized.includes("plan not found")) {
        return "No se encontró el plan seleccionado.";
    }

    if (normalized.includes("internal subscription not found")) {
        return "No se encontró la suscripción interna relacionada con esa operación.";
    }

    if (normalized.includes("does not have a provider subscription id")) {
        return "La suscripción todavía no está vinculada con una suscripción del proveedor. Primero reconciliá o recreá la suscripción.";
    }

    return text;
}

function stripJavaExceptionPrefix(text) {
    return text.replace(/^(?:[a-zA-Z_$][\w$]*\.)+[A-Za-z_$][\w$]*(?:Exception|Error)?:\s*/, "");
}

function parseEmbeddedJson(text) {
    const start = text.indexOf("{");
    const end = text.lastIndexOf("}");

    if (start === -1 || end <= start) {
        return null;
    }

    const embeddedJson = text.slice(start, end + 1);
    return parseJson(embeddedJson) ?? parseJson(embeddedJson.replaceAll('\\"', '"'));
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

function scheduleAutoRefresh() {
    window.clearTimeout(autoRefreshTimeoutId);
    autoRefreshTimeoutId = window.setTimeout(() => {
        loadDashboard({ silent: true });
    }, 300);
}

function connectLiveUpdates() {
    if (!window.EventSource) {
        return;
    }

    const source = new EventSource(endpoints.stateStream);
    source.addEventListener("state-changed", scheduleAutoRefresh);
    window.addEventListener("beforeunload", () => source.close());
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
        ["URL base de Mock Payment", info?.mock_payment_base_url ?? "-"],
        ["URL pública de StreamBox", info?.public_base_url ?? "-"],
        ["URL base de webhooks", info?.webhook_base_url ?? "-"],
        ["Ruta de webhook", info?.webhook_path ?? "-"],
        ["URL completa de webhook", info?.webhook_url ?? "-"],
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
        elements.subscriptionsTable.innerHTML = tableEmptyRow(8, "Todavía no hay suscripciones internas.");
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
                <td class="mono">${escapeHtml(subscription.providerExternalReference ?? "-")}</td>
                <td class="mono">${escapeHtml(subscription.providerSubscriptionId ?? "-")}</td>
                <td>${badge(subscription.status)}</td>
                <td>${escapeHtml(formatDate(subscription.updatedAt))}</td>
                <td>${renderSubscriptionAction(subscription)}</td>
            </tr>
        `)
        .join("");
}

function renderSubscriptionAction(subscription) {
    const needsReconciliation =
        !subscription.providerSubscriptionId ||
        subscription.status === "RECONCILIATION_NEEDED";

    if (!needsReconciliation) {
        return escapeHtml("-");
    }

    return `
        <button class="button button-secondary button-small" type="button" data-reconcile-subscription-id="${escapeHtml(subscription.id)}">
            Reconciliar
        </button>
    `;
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
    receivedWebhooksById = new Map(webhooks.map((webhook) => [webhook.id, webhook]));

    if (!webhooks.length) {
        elements.receivedWebhooksTable.innerHTML = tableEmptyRow(9, "Todavía no hay webhooks recibidos por StreamBox.");
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
                    <td>${badge(formatBoolean(webhook.validSignature))}</td>
                    <td>${badge(formatBoolean(webhook.processed))}</td>
                    <td>
                        ${
                webhook.error
                    ? `<span class="webhook-error">${escapeHtml(webhook.error)}</span>`
                    : escapeHtml("-")
            }
                    </td>
                    <td>${escapeHtml(formatDate(webhook.receivedAt))}</td>
                    <td>
                        <button class="button button-secondary button-small" type="button" data-webhook-id="${escapeHtml(webhook.id)}">
                            Ver webhook
                        </button>
                    </td>
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

async function createSubscription(event) {
    event.preventDefault();

    const request = {
        plan_id: elements.planSelect.value,
        payer_email: elements.payerEmailInput.value.trim(),
    };

    try {
        const response = await requestJson(endpoints.createSubscription, {
            method: "POST",
            body: JSON.stringify(request),
        });

        showMessage(
            `Suscripción ${response.subscription.id} creada con estado ${response.subscription.status}. Ahora podés pagarla.`
        );

        await loadDashboard({ silent: true });
    } catch (error) {
        console.error(error);
        showMessage(`No se pudo crear la suscripción: ${error.message}`, "error");
        await loadDashboard({ silent: true });
    }
}

async function paySubscription(event) {
    event.preventDefault();

    const subscriptionId = elements.paymentSubscriptionSelect.value;

    if (!subscriptionId) {
        showMessage("Primero tenés que seleccionar una suscripción.", "error");
        return;
    }

    try {
        const response = await requestJson(endpoints.paySubscription(subscriptionId), {
            method: "POST",
            body: JSON.stringify({
                card_number: elements.paymentCardNumberInput.value,
            }),
        });

        showMessage(
            `Pago generado. Suscripción: ${response.subscription.status}. Pago: ${response.payment.status}.`
        );

        await loadDashboard({ silent: true });
    } catch (error) {
        console.error(error);
        showMessage(`No se pudo pagar la suscripción: ${error.message}`, "error");
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
    const providerSubscriptions = subscriptions.filter((subscription) =>
        Boolean(subscription.providerSubscriptionId)
    );

    const selects = [
        elements.paymentSubscriptionSelect,
        elements.recurringSubscriptionSelect,
        elements.changePlanSubscriptionSelect,
        elements.cancelSubscriptionSelect,
    ];

    for (const select of selects) {
        const previousValue = select.value;

        if (!providerSubscriptions.length) {
            select.innerHTML = `<option value="">Sin suscripciones listas</option>`;
            continue;
        }

        select.innerHTML = providerSubscriptions
            .map((subscription) => `
                <option value="${escapeHtml(subscription.id)}">
                    ${escapeHtml(subscription.id)} · ${escapeHtml(subscription.planName)} · ${escapeHtml(subscription.status)}
                </option>
            `)
            .join("");

        const exists = providerSubscriptions.some((subscription) => subscription.id === previousValue);

        if (exists) {
            select.value = previousValue;
        }
    }
}

function renderHelpDocs(activeId = "what-is-streambox") {
    const activeDoc = helpDocs.find((doc) => doc.id === activeId) ?? helpDocs[0];

    elements.helpNav.innerHTML = helpDocs
        .map((doc) => `
            <button
                class="help-nav-button ${doc.id === activeDoc.id ? "active" : ""}"
                type="button"
                data-help-doc="${escapeHtml(doc.id)}"
            >
                <strong>${escapeHtml(doc.title)}</strong>
                <span>${escapeHtml(doc.description)}</span>
            </button>
        `)
        .join("");

    elements.helpContent.innerHTML = activeDoc.html;
}

function openHelpModal(docId = "what-is-streambox") {
    renderHelpDocs(docId);
    elements.helpModalBackdrop.classList.remove("hidden");
    document.body.classList.add("modal-open");
}

function closeHelpModal() {
    elements.helpModalBackdrop.classList.add("hidden");
    document.body.classList.remove("modal-open");
}

async function reconcileSubscription(subscriptionId) {
    try {
        const response = await requestJson(endpoints.reconcileSubscription(subscriptionId), {
            method: "POST",
        });

        showMessage(
            `Reconciliación terminada. Suscripción ${response.subscription.id}: ${response.subscription.status}.`
        );

        await loadDashboard({ silent: true });
    } catch (error) {
        console.error(error);
        showMessage(`No se pudo reconciliar la suscripción: ${error.message}`, "error");
        await loadDashboard({ silent: true });
    }
}

function openWebhookModal(webhook) {
    const eventLabel = webhook.action ?? webhook.type ?? webhook.id;

    elements.webhookModalTitle.textContent = `Webhook ${webhook.id}`;
    elements.webhookModalSubtitle.textContent = `${eventLabel} - ${formatDate(webhook.receivedAt)}`;
    elements.webhookPayloadBox.textContent = formatJsonBlock(webhook.payload);
    elements.webhookModalBackdrop.classList.remove("hidden");
    document.body.classList.add("modal-open");
}

function closeWebhookModal() {
    elements.webhookModalBackdrop.classList.add("hidden");
    document.body.classList.remove("modal-open");
}

elements.helpButton.addEventListener("click", () => openHelpModal());
elements.helpCloseButton.addEventListener("click", closeHelpModal);
elements.webhookCloseButton.addEventListener("click", closeWebhookModal);

elements.helpModalBackdrop.addEventListener("click", (event) => {
    if (event.target === elements.helpModalBackdrop) {
        closeHelpModal();
    }
});

elements.webhookModalBackdrop.addEventListener("click", (event) => {
    if (event.target === elements.webhookModalBackdrop) {
        closeWebhookModal();
    }
});

document.addEventListener("click", (event) => {
    const reconcileButton = event.target.closest("[data-reconcile-subscription-id]");

    if (reconcileButton) {
        reconcileSubscription(reconcileButton.dataset.reconcileSubscriptionId);
        return;
    }

    const webhookButton = event.target.closest("[data-webhook-id]");

    if (webhookButton) {
        const webhook = receivedWebhooksById.get(webhookButton.dataset.webhookId);

        if (webhook) {
            openWebhookModal(webhook);
        }

        return;
    }

    const helpDocButton = event.target.closest("[data-help-doc]");

    if (helpDocButton) {
        renderHelpDocs(helpDocButton.dataset.helpDoc);
    }
});

document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") {
        return;
    }

    if (!elements.webhookModalBackdrop.classList.contains("hidden")) {
        closeWebhookModal();
    } else if (!elements.helpModalBackdrop.classList.contains("hidden")) {
        closeHelpModal();
    }
});

elements.subscriptionForm.addEventListener("submit", createSubscription);
elements.paymentForm.addEventListener("submit", paySubscription);
elements.recurringChargeForm.addEventListener("submit", simulateRecurringCharge);
elements.changePlanForm.addEventListener("submit", changeSubscriptionPlan);
elements.cancelSubscriptionForm.addEventListener("submit", cancelSubscription);

elements.refreshButton.addEventListener("click", () => loadDashboard());
elements.resetButton.addEventListener("click", resetState);

connectLiveUpdates();
loadDashboard();
