# Qué código de StreamBox Demo conviene emular en un proyecto real

Esta guía explica qué partes de `subscription-demo-app` sirven como referencia para implementar pagos en una aplicación real, y qué partes son solamente código de demostración.

`subscription-demo-app` no intenta ser una aplicación productiva. Es un proyecto educativo que muestra el flujo completo desde el punto de vista de una aplicación principal.

La idea es usarlo como mapa para responder esta pregunta:

```txt
¿Qué código debería mirar y adaptar cuando integre pagos en mi proyecto real?
```

---

## Resumen rápido

En un proyecto real conviene emular principalmente estas piezas:

```txt
Cliente HTTP hacia la pasarela
DTOs de entrada/salida del proveedor
Servicio de billing/suscripciones
Controlador de webhooks
Validador de firma del webhook
Modelos internos de suscripción y pago
Registro de webhooks recibidos
Mapeo entre estados externos e internos
Eventos internos o logs de negocio
```

No conviene copiar tal cual:

```txt
DemoStore en memoria/archivo
Planes hardcodeados
Tarjetas ficticias
UI estática educativa
Endpoints específicos de simulación
```

---

## Qué representa cada capa

StreamBox Demo tiene dos tipos de código:

| Tipo de código | Sirve para proyecto real | Comentario |
|---|---:|---|
| Integración HTTP con el proveedor | Sí | Cambiaría el proveedor real, pero la idea se mantiene |
| Webhook receiver | Sí | Es una parte clave de cualquier integración real |
| Validación de firma | Sí | Adaptar a la firma del proveedor real |
| Estado interno de suscripción | Sí | En un proyecto real iría en base de datos |
| Estado interno de pagos | Sí | En un proyecto real iría en base de datos |
| Eventos internos | Sí | Puede convertirse en logs, auditoría o tabla de eventos |
| DemoStore | No como producción | Reemplazar por repositorios y base de datos |
| UI de StreamBox | Solo como referencia educativa | No es patrón productivo obligatorio |
| PlanCatalogService hardcodeado | Solo para demo | En producción los planes suelen estar en base de datos |
| Tarjetas ficticias | No | Solo existen porque el proveedor es mock |
| Acciones de simulación | No | Son útiles para aprender, no para producción |

---

# Código que conviene emular

## 1. Cliente HTTP hacia la pasarela

Archivo de referencia:

```txt
src/main/java/com/survila/subscriptiondemo/client/MockPaymentClient.java
```

Este código sirve como ejemplo de una capa dedicada a hablar con el proveedor externo.

En un proyecto real, esta capa podría llamarse:

```txt
PaymentProviderClient
MercadoPagoClient
StripeClient
MockPaymentClient
```

La idea importante es que el resto del sistema no debería armar URLs ni llamar directamente al proveedor desde cualquier lugar.

En vez de eso:

```txt
Controller
  ↓
Service
  ↓
PaymentProviderClient
  ↓
Proveedor externo
```

Ejemplo de responsabilidad:

```txt
createPreapproval()
getPreapproval()
getPayment()
cancelPreapproval()
changePlan()
```

En producción, esta clase debería manejar también:

```txt
timeouts
errores HTTP
reintentos si corresponde
logs
headers de autenticación
tokens del proveedor
mapeo de errores del proveedor
```

---

## 2. DTOs del proveedor

Carpeta de referencia:

```txt
src/main/java/com/survila/subscriptiondemo/dto/mock/
```

Estos DTOs representan el contrato con `mock-payment-service`.

En un proyecto real conviene tener DTOs específicos para el proveedor externo, por ejemplo:

```txt
MercadoPagoCreatePreapprovalRequest
MercadoPagoPreapprovalResponse
MercadoPagoPaymentResponse
StripeSubscriptionResponse
```

La idea importante es separar:

```txt
DTOs del proveedor
```

de:

```txt
modelos internos de la aplicación
```

No conviene guardar directamente en la base de datos todo lo que devuelve el proveedor sin mapearlo.

---

## 3. Servicio de suscripciones o billing

Archivo de referencia:

```txt
src/main/java/com/survila/subscriptiondemo/service/SubscriptionService.java
```

Este es uno de los archivos más importantes para mirar.

Muestra la lógica de orquestación:

```txt
Crear suscripción interna
        ↓
Crear suscripción en proveedor
        ↓
Guardar providerSubscriptionId
        ↓
Procesar pago inicial o esperar confirmación
        ↓
Registrar pago interno
        ↓
Actualizar estado interno
```

En un proyecto real, esta clase podría dividirse en servicios como:

```txt
BillingService
BusinessSubscriptionService
PaymentService
WebhookProcessingService
```

La idea importante es que la app real debe tener su propio estado interno. No alcanza con depender solamente del estado del proveedor.

Ejemplo:

```txt
BusinessSubscription.status = ACTIVE
BusinessSubscription.providerSubscriptionId = mock-preapproval-1
Payment.providerPaymentId = mock-payment-1
Payment.status = APPROVED
```

---

## 4. Controlador de webhooks

Archivo de referencia:

```txt
src/main/java/com/survila/subscriptiondemo/controller/MockPaymentWebhookController.java
```

Este código sí es muy importante para emular.

Muestra el patrón correcto:

```txt
Recibir webhook
        ↓
Extraer headers
        ↓
Validar firma
        ↓
Registrar webhook recibido
        ↓
Delegar procesamiento al servicio
        ↓
Marcar como procesado o fallido
```

En un proyecto real, este controlador puede llamarse:

```txt
PaymentWebhookController
MercadoPagoWebhookController
StripeWebhookController
```

Debe tener una responsabilidad pequeña:

```txt
recibir HTTP
validar datos mínimos
validar firma
llamar al servicio
responder HTTP 200 si se procesó correctamente
```

No conviene meter toda la lógica de negocio dentro del controller.

---

## 5. Validador de firma

Archivo de referencia:

```txt
src/main/java/com/survila/subscriptiondemo/service/WebhookSignatureVerifier.java
```

Este patrón sirve para producción, aunque el algoritmo exacto depende del proveedor real.

La idea importante:

```txt
No procesar un webhook sin validar que realmente vino del proveedor esperado.
```

En un proveedor real, la validación puede usar:

```txt
HMAC
firma con timestamp
secreto compartido
certificados
headers específicos del proveedor
```

El validador debería estar separado del controller para poder testearlo y reutilizarlo.

---

## 6. Procesamiento de webhooks

Método de referencia:

```txt
SubscriptionService.processWebhook(...)
```

Este código muestra una regla muy importante:

```txt
El webhook se trata como un aviso, no como fuente final de verdad.
```

El flujo recomendado es:

```txt
Webhook recibido
        ↓
Firma válida
        ↓
Leer type/action/data.id
        ↓
Consultar al proveedor:
            GET /payment/{id}
            GET /preapproval/{id}
        ↓
Actualizar estado interno
```

Esto evita confiar ciegamente en el body del webhook.

En producción, este procesamiento debería ser idempotente.

Es decir, si llega el mismo webhook dos veces, la aplicación no debería duplicar pagos ni romper el estado.

---

## 7. Modelo interno de suscripción

Archivo de referencia:

```txt
src/main/java/com/survila/subscriptiondemo/model/DemoSubscription.java
```

En un proyecto real, este modelo podría convertirse en una entidad JPA:

```txt
BusinessSubscription
```

Campos útiles para producción:

```txt
id
businessId
planId
provider
providerSubscriptionId
status
amount
currency
payerEmail
createdAt
updatedAt
cancelledAt
```

La idea importante es guardar tanto el dato interno como el dato externo:

```txt
id interno de la app
id externo del proveedor
```

Ejemplo:

```txt
id interno: business_subscription_id = 1
id externo: provider_subscription_id = mock-preapproval-1
```

---

## 8. Modelo interno de pago

Archivo de referencia:

```txt
src/main/java/com/survila/subscriptiondemo/model/DemoPayment.java
```

En un proyecto real, este modelo podría convertirse en una entidad:

```txt
BusinessPayment
SubscriptionPayment
PaymentTransaction
```

Campos útiles:

```txt
id
subscriptionId
providerPaymentId
status
statusDetail
amount
currency
createdAt
rawProviderStatus
```

La idea importante es que los pagos deben registrarse internamente.

No conviene depender solo del historial que tenga el proveedor.

---

## 9. Registro de webhooks recibidos

Archivo de referencia:

```txt
src/main/java/com/survila/subscriptiondemo/model/DemoReceivedWebhook.java
```

Esto es muy útil para producción.

Un proyecto real podría tener una tabla:

```txt
payment_webhook_event
```

Campos útiles:

```txt
id
provider
requestId
eventId
type
action
dataId
validSignature
processed
error
receivedAt
processedAt
payloadRaw
```

Esto ayuda a:

```txt
depurar integraciones
evitar procesar duplicados
auditar problemas de pago
saber por qué una suscripción no cambió de estado
```

StreamBox Demo guarda el payload completo para poder inspeccionarlo desde la UI. En producción conviene guardar ese payload raw con cuidado, evitando exponer datos sensibles innecesarios.

---

## 10. Mapeo de estados

Referencia:

```txt
SubscriptionService.mapPreapprovalStatus(...)
SubscriptionService.mapPaymentStatus(...)
```

Esta idea es clave.

Cada proveedor tiene sus propios estados. La aplicación real debería mapearlos a estados internos propios.

Ejemplo:

```txt
Proveedor: authorized
App interna: ACTIVE

Proveedor: payment_failed
App interna: PAYMENT_FAILED

Proveedor: cancelled
App interna: CANCELLED
```

Esto evita que toda la aplicación dependa directamente del vocabulario de un proveedor específico.

---

# Código que no conviene copiar tal cual

## 1. DemoStore

Archivo:

```txt
src/main/java/com/survila/subscriptiondemo/store/DemoStore.java
```

Sirve para la demo porque permite persistencia simple en memoria o archivo.

En producción debería reemplazarse por:

```txt
JPA repositories
base de datos
transacciones
índices
constraints
migraciones Flyway/Liquibase
```

Ejemplo:

```txt
BusinessSubscriptionRepository
PaymentRepository
WebhookEventRepository
```

---

## 2. PlanCatalogService hardcodeado

Archivo:

```txt
src/main/java/com/survila/subscriptiondemo/service/PlanCatalogService.java
```

Sirve para simplificar la demo.

En producción, los planes deberían venir de:

```txt
base de datos
configuración administrable
panel de administración
tabla platform_plan
```

---

## 3. Tarjetas ficticias

Las tarjetas ficticias solo existen porque `mock-payment-service` es un simulador.

En producción, la aplicación principal normalmente no debería manejar números de tarjeta directamente salvo que cumpla requisitos muy estrictos de seguridad y PCI.

En muchas integraciones reales, el usuario paga en:

```txt
checkout hospedado por el proveedor
formulario tokenizado del proveedor
SDK oficial
```

---

## 4. UI estática

La UI de StreamBox sirve para explicar el flujo.

En una aplicación real, la UI podría estar en:

```txt
Next.js
React
Angular
Vue
Astro
mobile app
```

El patrón importante no es la UI, sino el flujo backend.

---

# Estructura sugerida para un proyecto real

Una posible estructura backend:

```txt
billing/
  controller/
    BillingController.java
    PaymentWebhookController.java

  service/
    BillingService.java
    PaymentWebhookService.java
    PaymentProviderService.java

  provider/
    PaymentProviderClient.java
    MercadoPagoClient.java
    MockPaymentClient.java

  dto/
    StartSubscriptionRequest.java
    BillingSubscriptionResponse.java

  dto/provider/
    ProviderCreateSubscriptionRequest.java
    ProviderSubscriptionResponse.java
    ProviderPaymentResponse.java

  model/
    BusinessSubscription.java
    BusinessPayment.java
    PaymentWebhookEvent.java

  repository/
    BusinessSubscriptionRepository.java
    BusinessPaymentRepository.java
    PaymentWebhookEventRepository.java

  mapper/
    PaymentStatusMapper.java
    SubscriptionStatusMapper.java
```

---

# Flujo mínimo para emular en producción

## Crear una suscripción

```txt
1. Validar plan interno.
2. Crear suscripción local en CREATING.
3. Llamar al proveedor.
4. Guardar providerSubscriptionId.
5. Cambiar la suscripción local a PENDING.
6. Devolver init_point o estado inicial al frontend.
```

## Recibir webhook

```txt
1. Recibir request.
2. Extraer headers.
3. Validar firma.
4. Registrar evento recibido.
5. Verificar idempotencia.
6. Consultar recurso actualizado al proveedor.
7. Actualizar entidad interna.
8. Marcar webhook como procesado.
9. Responder 200 OK.
```

## Procesar pago

```txt
1. Consultar pago al proveedor.
2. Buscar suscripción interna por providerSubscriptionId.
3. Verificar si ese providerPaymentId ya fue guardado.
4. Crear o actualizar pago interno.
5. Mapear estado externo a estado interno.
6. Actualizar suscripción interna.
```

---

# Checklist para adaptar este código a un proyecto real

```txt
Crear entidades reales en base de datos.
Crear repositorios.
Crear migraciones.
Crear PaymentProviderClient.
Crear DTOs del proveedor.
Crear BillingService.
Crear WebhookController.
Crear WebhookSignatureVerifier.
Crear tabla de webhooks recibidos.
Implementar idempotencia.
Mapear estados externos a internos.
Agregar logs.
Agregar tests.
Agregar configuración por variables de entorno.
```

---

# Relación con chatbot-platform-api

Si este patrón se lleva a un proyecto como `chatbot-platform-api`, el flujo podría ser:

```txt
BUSINESS_ADMIN elige plan
        ↓
chatbot-platform-api crea BusinessSubscription PENDING
        ↓
chatbot-platform-api llama al proveedor
        ↓
guarda providerSubscriptionId
        ↓
recibe webhook
        ↓
valida firma
        ↓
consulta pago/suscripción al proveedor
        ↓
actualiza BusinessSubscription
        ↓
habilita, limita o suspende funcionalidades según el plan y el estado de pago
```

Entidades posibles:

```txt
PlatformPlan
BusinessSubscription
BusinessPayment
PaymentWebhookEvent
```

Estados posibles:

```txt
PENDING
ACTIVE
PAYMENT_FAILED
PAUSED
CANCELLED
```

---

# Regla final

El código más importante para emular no es el que simula pagos, sino el que muestra cómo una aplicación real debe pensar la integración:

```txt
estado interno propio
provider IDs guardados
webhooks firmados
consulta posterior al proveedor
idempotencia
mapeo de estados
auditoría de eventos
```

StreamBox Demo es útil porque muestra ese flujo completo en una versión pequeña, visible y fácil de probar.
