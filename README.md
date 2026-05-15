# Subscription Demo App

Aplicación demo local para aprender cómo una aplicación principal puede integrarse con un servicio de pagos simulado.

La UI representa una plataforma ficticia de streaming llamada **StreamBox Demo**. Permite elegir un plan, crear una suscripción, pagarla con tarjeta ficticia, recibir webhooks firmados desde `mock-payment-service` y mostrar cómo la aplicación principal actualiza su propio estado interno.

Está pensada para funcionar junto con:

```txt
mock-payment-service
```

---

## Disclaimer

This project is not affiliated with Mercado Pago, Stripe, PayPal, or any real payment provider.

It is a local demo application for development and learning purposes only.

It does not process real payments, real cards, or real subscriptions.

---

## Objetivo del proyecto

El objetivo de `subscription-demo-app` es mostrar el lado de la **aplicación principal** en un flujo de pagos.

Mientras `mock-payment-service` simula la pasarela de pago, StreamBox Demo simula una app real que:

```txt
Crea una suscripción interna
        ↓
Llama a la pasarela simulada para crear una preapproval
        ↓
Guarda el provider_subscription_id
        ↓
Permite pagar una suscripción existente con tarjeta ficticia
        ↓
Recibe un webhook firmado
        ↓
Valida la firma
        ↓
Consulta el recurso actualizado en la pasarela
        ↓
Actualiza su estado interno
```

---

## Qué representa cada proyecto

| Proyecto | Rol |
|---|---|
| `subscription-demo-app` | Aplicación principal de ejemplo |
| `mock-payment-service` | Pasarela de pagos simulada |
| Mock Payment Studio | Panel para inspeccionar el lado del proveedor |
| StreamBox Demo UI | Panel para inspeccionar el lado de la aplicación principal |

---

## Características principales

- UI local servida desde Spring Boot.
- Catálogo de planes hardcodeados.
- Creación de suscripciones internas.
- Flujo separado entre crear suscripción y pagar.
- Integración HTTP con `mock-payment-service`.
- Pago inicial con tarjeta ficticia sobre una suscripción existente.
- Simulación de cobros recurrentes.
- Cambio de plan.
- Cancelación de suscripción.
- Recepción de webhooks firmados.
- Validación de firma HMAC SHA-256.
- Consulta posterior de pagos y suscripciones al proveedor.
- Estado interno propio de la aplicación principal.
- Registro de eventos internos.
- Registro de webhooks recibidos por StreamBox.
- Visualización de configuración real de integración.
- Persistencia en memoria o archivo local.
- Dockerfile y Docker Compose.
- Puerto publicado configurable con `DEMO_APP_HOST_PORT`.
- Variables Docker configurables desde `.env` o desde el comando `docker compose up`.
- Red Docker externa compartida para integrarse con `mock-payment-service` en contenedores separados.
- Ayuda integrada en la UI.
- Documentación sobre qué partes del código sirven como referencia para un proyecto real.
- Menú `Docs` integrado en la UI.
- Documentación HTML navegable servida desde `/docs/`.
- Índice de documentación integrado.
- Guía rápida de uso.
- Guía de Docker y configuración.
- Guía de webhooks y estado interno.
- Guía de troubleshooting.
- Guía de escenarios de prueba.
- Guía de seguridad y límites.
- Guía de arquitectura interna.
- Guía para migrar a una pasarela real.
- Referencia de endpoints.
- Guía de responsabilidades legales, privacidad y prevención de fraude.
- Recorrido comentado del código de pagos.
- Licencia MIT.

---

## Tecnologías

- Java
- Spring Boot
- Spring Web MVC
- Bean Validation
- Maven
- Docker
- HTML, CSS y JavaScript vanilla

---

## Puertos por defecto

| Servicio | URL |
|---|---|
| StreamBox Demo | `http://localhost:8080/` |
| Mock Payment Service | `http://localhost:9090/` |
| Mock Payment Studio | `http://localhost:9090/studio/` |

---

# Requisitos

Para usar esta app se necesita tener corriendo `mock-payment-service`.

También se recomienda que ambos proyectos estén conectados a una red Docker externa compartida cuando se ejecutan en contenedores separados.

Crear la red una sola vez:

```bash
docker network create payment-demo-network
```

Si la red ya existe, Docker informará que ya está creada.

---

# Ejecutar el proyecto

## Opción 1: con Maven

Desde la raíz del proyecto:

```bash
./mvnw spring-boot:run
```

Si se usa Maven instalado globalmente:

```bash
mvn spring-boot:run
```

La app quedará disponible en:

```txt
http://localhost:8080/
```

En este modo, si `mock-payment-service` también corre localmente con Maven, se pueden usar URLs con `localhost`.

---

## Opción 2: con Docker Compose

```bash
docker compose up --build
```

La app quedará disponible en:

```txt
http://localhost:8080/
```

Por defecto, el `docker-compose.yml` usa persistencia en archivo mediante volumen local:

```txt
./data:/app/data
```

---

## Ejecutar en otro puerto publicado

Si el puerto `8080` de tu máquina ya está ocupado, se puede publicar StreamBox en otro puerto usando `DEMO_APP_HOST_PORT`.

Ejemplo:

```bash
DEMO_APP_HOST_PORT=8085 docker compose up --build
```

En ese caso, el navegador abre:

```txt
http://localhost:8085/
```

Pero internamente el contenedor sigue escuchando en:

```txt
subscription-demo-app:8080
```

Eso permite que `mock-payment-service` le siga enviando webhooks a:

```txt
http://subscription-demo-app:8080/api/webhooks/mock-payment
```

---

## Variables configurables desde Docker Compose

El contenedor puede configurarse con variables de entorno.

Ejemplo:

```bash
DEMO_APP_HOST_PORT=8085 \
MOCK_PAYMENT_WEBHOOK_SECRET=otro-secret \
DEMO_APP_STORAGE_TYPE=memory \
docker compose up --build
```

También se puede usar un archivo `.env`.

---

# Docker y red compartida

Cuando `subscription-demo-app` y `mock-payment-service` corren en contenedores separados, deben estar en la misma red Docker externa.

Ejemplo recomendado para `subscription-demo-app`:

```yaml
services:
  subscription-demo-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: subscription-demo-app
    ports:
      - "${DEMO_APP_HOST_PORT:-8080}:8080"
    environment:
      MOCK_PAYMENT_BASE_URL: "${MOCK_PAYMENT_BASE_URL:-http://mock-payment-service:9090}"
      MOCK_PAYMENT_WEBHOOK_SECRET: "${MOCK_PAYMENT_WEBHOOK_SECRET:-dev-secret}"

      DEMO_APP_PUBLIC_BASE_URL: "${DEMO_APP_PUBLIC_BASE_URL:-http://localhost:${DEMO_APP_HOST_PORT:-8080}}"
      DEMO_APP_WEBHOOK_BASE_URL: "${DEMO_APP_WEBHOOK_BASE_URL:-http://subscription-demo-app:8080}"

      DEMO_APP_STORAGE_TYPE: "${DEMO_APP_STORAGE_TYPE:-file}"
      DEMO_APP_STORAGE_FILE_PATH: "${DEMO_APP_STORAGE_FILE_PATH:-/app/data/subscription-demo-state.bin}"
    volumes:
      - ./data:/app/data
    networks:
      - payment-demo-network

networks:
  payment-demo-network:
    external: true
```

Regla práctica:

```txt
Navegador → contenedor:
  http://localhost:8080
  http://localhost:8085
  http://localhost:9090

Contenedor → contenedor:
  http://container-name:port
  http://service-name:port

Contenedor → máquina host:
  http://host.docker.internal:port
```

Ejemplos:

```txt
StreamBox Demo container → Mock Payment container:
http://mock-payment-service:9090

Mock Payment container → StreamBox Demo container:
http://subscription-demo-app:8080/api/webhooks/mock-payment
```

Aunque StreamBox se abra desde el navegador en:

```txt
http://localhost:8085/
```

el webhook entre contenedores debería seguir apuntando a:

```txt
http://subscription-demo-app:8080/api/webhooks/mock-payment
```

---

# Configuración

El archivo principal es:

```txt
src/main/resources/application.yaml
```

Ejemplo:

```yaml
spring:
  application:
    name: subscription-demo-app

server:
  port: 8080

demo-app:
  public-base-url: ${DEMO_APP_PUBLIC_BASE_URL:http://localhost:8080}
  webhook-base-url: ${DEMO_APP_WEBHOOK_BASE_URL:http://localhost:8080}
  mock-payment:
    base-url: ${MOCK_PAYMENT_BASE_URL:http://localhost:9090}
    webhook-secret: ${MOCK_PAYMENT_WEBHOOK_SECRET:dev-secret}
  storage:
    type: ${DEMO_APP_STORAGE_TYPE:memory}
    file-path: ${DEMO_APP_STORAGE_FILE_PATH:./data/subscription-demo-state.bin}
```

---

## Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DEMO_APP_HOST_PORT` | Puerto publicado en la máquina host cuando se usa Docker Compose | `8085` |
| `DEMO_APP_PUBLIC_BASE_URL` | URL pública de StreamBox para el navegador | `http://localhost:8085` |
| `DEMO_APP_WEBHOOK_BASE_URL` | URL que usa Mock Payment Service para enviar webhooks a StreamBox | `http://subscription-demo-app:8080` |
| `MOCK_PAYMENT_BASE_URL` | URL que usa StreamBox para llamar a Mock Payment Service | `http://mock-payment-service:9090` |
| `MOCK_PAYMENT_WEBHOOK_SECRET` | Secret compartido para validar webhooks | `dev-secret` |
| `DEMO_APP_STORAGE_TYPE` | Tipo de persistencia: `memory` o `file` | `file` |
| `DEMO_APP_STORAGE_FILE_PATH` | Ruta del archivo de estado | `/app/data/subscription-demo-state.bin` |

---

## Ejemplo `.env`

Se puede crear un archivo `.env` en la raíz del proyecto:

```env
# Puerto publicado en la máquina host.
# Si se deja vacío, Docker Compose usa 8080.
DEMO_APP_HOST_PORT=8085

# URL que usa StreamBox desde el navegador.
DEMO_APP_PUBLIC_BASE_URL=http://localhost:8085

# URL que usa Mock Payment Service para enviar webhooks a StreamBox.
# En Docker, debe usar el nombre del contenedor de StreamBox y el puerto interno.
DEMO_APP_WEBHOOK_BASE_URL=http://subscription-demo-app:8080

# URL que usa StreamBox para llamar a Mock Payment Service.
# En Docker, debe usar el nombre del contenedor de Mock Payment Service.
MOCK_PAYMENT_BASE_URL=http://mock-payment-service:9090

# Debe coincidir con el secret configurado en mock-payment-service.
MOCK_PAYMENT_WEBHOOK_SECRET=dev-secret

# Persistencia: memory o file.
DEMO_APP_STORAGE_TYPE=file

# Ruta interna del archivo de estado cuando se usa Docker.
DEMO_APP_STORAGE_FILE_PATH=/app/data/subscription-demo-state.bin
```

Luego:

```bash
docker compose up --build
```

---

# Modos de persistencia

La app soporta dos modos de almacenamiento:

| Modo | Descripción |
|---|---|
| `memory` | Guarda todo en memoria. Al reiniciar se pierde el estado. |
| `file` | Guarda el estado en un archivo local. Al reiniciar conserva datos. |

---

## Modo memory

```yaml
demo-app:
  storage:
    type: memory
```

Con Docker Compose:

```bash
DEMO_APP_STORAGE_TYPE=memory docker compose up --build
```

Ideal para pruebas rápidas.

---

## Modo file

```yaml
demo-app:
  storage:
    type: file
    file-path: ./data/subscription-demo-state.bin
```

En Docker Compose se usa:

```yaml
environment:
  DEMO_APP_STORAGE_TYPE: file
  DEMO_APP_STORAGE_FILE_PATH: /app/data/subscription-demo-state.bin

volumes:
  - ./data:/app/data
```

Ideal para conservar datos entre reinicios.

---

# Health check

Endpoint:

```txt
GET /health
```

Ejemplo con puerto por defecto:

```bash
curl http://localhost:8080/health
```

Ejemplo con puerto publicado alternativo:

```bash
curl http://localhost:8085/health
```

Respuesta esperada:

```txt
OK
```

---

# UI de StreamBox Demo

La UI se accede desde:

```txt
http://localhost:8080/
```

Si se publicó en otro puerto:

```txt
http://localhost:8085/
```

La UI permite:

- Ver estado general de la aplicación principal.
- Ver configuración real de integración.
- Crear una suscripción.
- Pagar una suscripción existente con tarjeta ficticia.
- Simular cobros recurrentes.
- Cambiar el plan de una suscripción.
- Cancelar una suscripción.
- Ver planes disponibles.
- Ver suscripciones internas.
- Ver pagos internos.
- Ver webhooks recibidos por StreamBox.
- Ver eventos internos.
- Consultar ayuda integrada.

---

# Planes disponibles

La app incluye planes hardcodeados.

| ID | Nombre | Monto |
|---|---|---|
| `basic` | Plan Básico | `10000 ARS` |
| `pro` | Plan Pro | `20000 ARS` |
| `enterprise` | Plan Empresarial | `50000 ARS` |

Endpoint:

```txt
GET /api/plans
```

Ejemplo con puerto por defecto:

```bash
curl http://localhost:8080/api/plans
```

Ejemplo con puerto publicado alternativo:

```bash
curl http://localhost:8085/api/plans
```

---

# Flujo principal

## 1. Resetear estado

Con puerto por defecto:

```bash
curl -X DELETE http://localhost:8080/api/demo/state
```

Con puerto publicado alternativo:

```bash
curl -X DELETE http://localhost:8085/api/demo/state
```

---

## 2. Crear suscripción

Endpoint:

```txt
POST /api/subscriptions
```

Ejemplo con puerto por defecto:

```bash
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "plan_id": "basic",
    "payer_email": "cliente@test.com"
  }'
```

Ejemplo con puerto publicado alternativo:

```bash
curl -X POST http://localhost:8085/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "plan_id": "basic",
    "payer_email": "cliente@test.com"
  }'
```

Resultado esperado:

```txt
subscription.status = PENDING
provider_subscription_id = mock-preapproval-...
```

La app internamente hace:

```txt
1. Crea una suscripción interna PENDING.
2. Llama a POST /preapproval en mock-payment-service.
3. Guarda el provider_subscription_id.
4. Deja la suscripción lista para pagar.
```

---

## 3. Pagar suscripción

Endpoint:

```txt
POST /api/subscriptions/{id}/pay
```

Ejemplo con puerto por defecto:

```bash
curl -X POST http://localhost:8080/api/subscriptions/demo-subscription-1/pay \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111"
  }'
```

Ejemplo con puerto publicado alternativo:

```bash
curl -X POST http://localhost:8085/api/subscriptions/demo-subscription-1/pay \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111"
  }'
```

Resultado esperado con tarjeta aprobada:

```txt
subscription.status = ACTIVE
payment.status = APPROVED
```

La app internamente hace:

```txt
1. Busca la suscripción interna.
2. Usa el provider_subscription_id guardado.
3. Simula el pago con tarjeta ficticia en mock-payment-service.
4. Registra el pago interno.
5. Recibe webhook.
6. Valida firma.
7. Consulta recurso actualizado.
8. Actualiza estado interno.
```

---

## Endpoint combinado legacy

También existe el endpoint combinado anterior:

```txt
POST /api/subscriptions/start
```

Ese endpoint crea la suscripción y simula el pago inicial en una sola llamada. Se mantiene por compatibilidad, pero el flujo principal de la UI usa los pasos separados:

```txt
POST /api/subscriptions
POST /api/subscriptions/{id}/pay
```

---

# Tarjetas ficticias

Las tarjetas ficticias pertenecen a `mock-payment-service`, pero StreamBox las usa en el paso **Pagar** para simular pagos sobre una suscripción existente.

| Tarjeta | Resultado |
|---|---|
| `4111111111111111` | Pago aprobado |
| `4000000000000002` | Tarjeta sin fondos |
| `4000000000009995` | Número de tarjeta inválido |
| `4000000000000069` | Tarjeta vencida |
| `4000000000000341` | Pago pendiente |
| `4000000000000259` | Error genérico |

---

# Acciones sobre suscripciones

## Simular cobro recurrente

Endpoint:

```txt
POST /api/subscriptions/{id}/simulate-recurring-charge
```

Ejemplo aprobado:

```bash
curl -X POST http://localhost:8080/api/subscriptions/demo-subscription-1/simulate-recurring-charge \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111"
  }'
```

Ejemplo rechazado:

```bash
curl -X POST http://localhost:8080/api/subscriptions/demo-subscription-1/simulate-recurring-charge \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4000000000000002"
  }'
```

Si StreamBox está publicado en otro puerto, reemplazar:

```txt
http://localhost:8080
```

por:

```txt
http://localhost:8085
```

---

## Cambiar plan

Endpoint:

```txt
POST /api/subscriptions/{id}/change-plan
```

Ejemplo:

```bash
curl -X POST http://localhost:8080/api/subscriptions/demo-subscription-1/change-plan \
  -H "Content-Type: application/json" \
  -d '{
    "plan_id": "pro"
  }'
```

Resultado esperado:

```txt
subscription.planId = pro
subscription.planName = Plan Pro
```

---

## Cancelar suscripción

Endpoint:

```txt
POST /api/subscriptions/{id}/cancel
```

Ejemplo:

```bash
curl -X POST http://localhost:8080/api/subscriptions/demo-subscription-1/cancel
```

Resultado esperado:

```txt
subscription.status = CANCELLED
```

---

# Webhooks recibidos por StreamBox

StreamBox recibe webhooks desde `mock-payment-service` en:

```txt
POST /api/webhooks/mock-payment
```

El webhook incluye:

```txt
x-request-id
x-signature
```

StreamBox valida la firma usando el secret configurado en:

```txt
MOCK_PAYMENT_WEBHOOK_SECRET
```

Después de validar la firma, StreamBox consulta el recurso actualizado en `mock-payment-service`.

Para eventos de pago:

```txt
GET /payment/{id}
```

Para eventos de suscripción:

```txt
GET /preapproval/{id}
```

---

## Eventos procesados

| type | action | data.id representa |
|---|---|---|
| `payment` | `payment.created` | ID del pago |
| `preapproval` | `preapproval.updated` | ID de la suscripción |
| `preapproval` | `preapproval.cancelled` | ID de la suscripción |

---

## Tabla de webhooks recibidos

La UI muestra una sección:

```txt
Webhooks recibidos por StreamBox
```

Columnas:

| Columna | Significado |
|---|---|
| ID interno | ID propio de StreamBox para el webhook recibido |
| Request ID | ID que permite relacionar la entrega con Mock Payment Studio |
| Evento | `action` y `type` del webhook |
| Data ID | ID del recurso informado por el webhook |
| Firma válida | Si la firma HMAC fue válida |
| Procesado | Si StreamBox pudo aplicar la lógica interna |
| Error | Error si el procesamiento falló |
| Recibido | Fecha de recepción |

---

# Relación con Mock Payment Studio

Cuando StreamBox recibe webhooks, en Mock Payment Studio se puede ver:

```txt
Webhook saliente delivered=true
```

Y en trazabilidad puede aparecer:

```txt
received internally=false
```

Eso es correcto.

Significa que el webhook fue entregado a StreamBox, no al receptor interno del mock.

---

# Estado interno

## Consultar estado general

Con puerto por defecto:

```bash
curl http://localhost:8080/api/demo/state
```

Con puerto publicado alternativo:

```bash
curl http://localhost:8085/api/demo/state
```

Respuesta ejemplo:

```json
{
  "subscriptions_count": 1,
  "payments_count": 1,
  "events_count": 8,
  "received_webhooks_count": 1,
  "storage_type": "file",
  "state_file_path": "/app/data/subscription-demo-state.bin"
}
```

---

## Resetear estado

```bash
curl -X DELETE http://localhost:8080/api/demo/state
```

Esto borra:

```txt
suscripciones internas
pagos internos
eventos internos
webhooks recibidos
```

Y reinicia los contadores internos.

---

# Información de integración

Endpoint:

```txt
GET /api/demo/info
```

Ejemplo con puerto por defecto:

```bash
curl http://localhost:8080/api/demo/info
```

Ejemplo con puerto publicado alternativo:

```bash
curl http://localhost:8085/api/demo/info
```

Respuesta esperada en Docker con puerto por defecto:

```json
{
  "name": "subscription-demo-app",
  "public_base_url": "http://localhost:8080",
  "webhook_base_url": "http://subscription-demo-app:8080",
  "mock_payment_base_url": "http://mock-payment-service:9090",
  "webhook_path": "/api/webhooks/mock-payment",
  "webhook_url": "http://subscription-demo-app:8080/api/webhooks/mock-payment"
}
```

Respuesta esperada en Docker usando `DEMO_APP_HOST_PORT=8085` y `DEMO_APP_PUBLIC_BASE_URL=http://localhost:8085`:

```json
{
  "name": "subscription-demo-app",
  "public_base_url": "http://localhost:8085",
  "webhook_base_url": "http://subscription-demo-app:8080",
  "mock_payment_base_url": "http://mock-payment-service:9090",
  "webhook_path": "/api/webhooks/mock-payment",
  "webhook_url": "http://subscription-demo-app:8080/api/webhooks/mock-payment"
}
```

---

# Endpoints disponibles

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/health` | Health check |
| `GET` | `/` | UI de StreamBox Demo |
| `GET` | `/api/demo/info` | Configuración real de integración |
| `GET` | `/api/demo/state` | Estado general de la demo |
| `DELETE` | `/api/demo/state` | Resetear estado |
| `GET` | `/api/plans` | Listar planes disponibles |
| `POST` | `/api/subscriptions` | Crear suscripción interna y preapproval |
| `POST` | `/api/subscriptions/{id}/pay` | Pagar una suscripción existente con tarjeta ficticia |
| `POST` | `/api/subscriptions/start` | Crear suscripción y simular pago inicial en una sola llamada legacy |
| `POST` | `/api/subscriptions/{id}/simulate-recurring-charge` | Simular cobro recurrente |
| `POST` | `/api/subscriptions/{id}/change-plan` | Cambiar plan |
| `POST` | `/api/subscriptions/{id}/cancel` | Cancelar suscripción |
| `GET` | `/api/demo/subscriptions` | Listar suscripciones internas |
| `GET` | `/api/demo/payments` | Listar pagos internos |
| `GET` | `/api/demo/events` | Listar eventos internos |
| `GET` | `/api/demo/webhooks` | Listar webhooks recibidos por StreamBox |
| `POST` | `/api/webhooks/mock-payment` | Recibir webhooks desde Mock Payment Service |

Los endpoints se consumen desde la máquina host usando el puerto publicado.

Por defecto:

```txt
http://localhost:8080
```

Con puerto personalizado:

```txt
http://localhost:{DEMO_APP_HOST_PORT}
```

Desde otro contenedor en la misma red Docker:

```txt
http://subscription-demo-app:8080
```

---

# Prueba completa recomendada

Con `mock-payment-service` y `subscription-demo-app` corriendo en Docker:

## 1. Resetear StreamBox

```bash
curl -X DELETE http://localhost:8080/api/demo/state
```

Si StreamBox está publicado en otro puerto:

```bash
curl -X DELETE http://localhost:8085/api/demo/state
```

---

## 2. Crear suscripción

```bash
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "plan_id": "basic",
    "payer_email": "cliente@test.com"
  }'
```

Si StreamBox está publicado en otro puerto:

```bash
curl -X POST http://localhost:8085/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "plan_id": "basic",
    "payer_email": "cliente@test.com"
  }'
```

---

## 3. Pagar suscripción

```bash
curl -X POST http://localhost:8080/api/subscriptions/demo-subscription-1/pay \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111"
  }'
```

Si StreamBox está publicado en otro puerto:

```bash
curl -X POST http://localhost:8085/api/subscriptions/demo-subscription-1/pay \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111"
  }'
```

---

## 4. Ver webhooks recibidos por StreamBox

```bash
curl http://localhost:8080/api/demo/webhooks
```

Debe aparecer un webhook con:

```txt
validSignature = true
processed = true
action = payment.created
```

---

## 5. Ver eventos internos

```bash
curl http://localhost:8080/api/demo/events
```

Buscar eventos como:

```txt
SUBSCRIPTION_CREATED
PROVIDER_PREAPPROVAL_CREATED
PAYMENT_CREATED
WEBHOOK_RECEIVED
WEBHOOK_PAYMENT_PROCESSED
SUBSCRIPTION_UPDATED_FROM_WEBHOOK
```

---

## 6. Ver en Mock Payment Studio

Abrir:

```txt
http://localhost:9090/studio/
```

Revisar:

```txt
Webhooks salientes → delivered=true
Trazabilidad → received internally=false
```

Eso es correcto si el webhook fue entregado a StreamBox.

---

# Errores comunes

## El puerto 8080 ya está ocupado

Usar otro puerto publicado:

```bash
DEMO_APP_HOST_PORT=8085 docker compose up --build
```

Abrir:

```txt
http://localhost:8085/
```

Recordatorio:

```txt
Host/navegador:
http://localhost:8085

Otros contenedores:
http://subscription-demo-app:8080
```

---

## Mock Payment no puede enviar webhooks a StreamBox

Revisar:

```txt
1. Ambos contenedores están en la red payment-demo-network.
2. DEMO_APP_WEBHOOK_BASE_URL usa http://subscription-demo-app:8080.
3. El endpoint /api/webhooks/mock-payment responde.
4. El secret coincide en ambos proyectos.
```

Verificar red:

```bash
docker network inspect payment-demo-network
```

Deben aparecer:

```txt
mock-payment-service
subscription-demo-app
```

---

## StreamBox no puede llamar a Mock Payment Service

Si StreamBox corre en Docker, revisar que use:

```txt
MOCK_PAYMENT_BASE_URL=http://mock-payment-service:9090
```

No debería usar:

```txt
http://localhost:9090
```

dentro del contenedor.

Si StreamBox corre directamente en la máquina host y Mock Payment Service está publicado en otro puerto, por ejemplo `9095`, usar:

```txt
MOCK_PAYMENT_BASE_URL=http://localhost:9095
```

---

## El secret de webhooks no coincide

StreamBox valida los webhooks usando:

```txt
MOCK_PAYMENT_WEBHOOK_SECRET
```

Ese valor debe coincidir con el secret configurado en `mock-payment-service`.

Ejemplo:

```txt
mock-payment-service:
MOCK_PAYMENT_WEBHOOK_SECRET=dev-secret

subscription-demo-app:
MOCK_PAYMENT_WEBHOOK_SECRET=dev-secret
```

Si no coinciden, StreamBox puede registrar el webhook como firma inválida o rechazarlo.

---

## El modal de ayuda aparece siempre o no se puede cerrar

Verificar que `styles.css` tenga una regla como:

```css
.modal-backdrop.hidden {
    display: none;
}
```

---

## Datos viejos o estado corrupto

Si se usa persistencia en archivo y el estado quedó inconsistente, se puede resetear desde la UI o borrar la carpeta local:

```txt
./data/
```

Luego levantar nuevamente:

```bash
docker compose up --build
```

---

# Archivos ignorados recomendados

```gitignore
target/
data/
.env
.idea/
*.iml
.DS_Store
```

---

# Documentación adicional

StreamBox Demo incluye documentación integrada en la UI.

Con la aplicación levantada, abrir:

```txt
http://localhost:8080/docs/
```

Si StreamBox fue publicado en otro puerto, por ejemplo `8085`:

```txt
http://localhost:8085/docs/
```

## Documentación integrada

| Página | Descripción |
|---|---|
| `/docs/index.html` | Índice de documentación |
| `/docs/quick-start.html` | Guía rápida de uso |
| `/docs/docker-configuration.html` | Docker, puertos, variables y red compartida |
| `/docs/webhooks-and-internal-state.html` | Webhooks, firma y estado interno |
| `/docs/troubleshooting.html` | Diagnóstico de errores comunes |
| `/docs/testing-scenarios.html` | Escenarios de prueba |
| `/docs/security-and-limitations.html` | Seguridad y límites de la demo |
| `/docs/internal-architecture.html` | Arquitectura interna |
| `/docs/migrate-to-real-payment-provider.html` | Migración hacia una pasarela real |
| `/docs/api-reference.html` | Referencia de endpoints |
| `/docs/legal-responsibilities.html` | Responsabilidades legales, privacidad y fraude |
| `/docs/payment-code-walkthrough.html` | Recorrido comentado del código de pagos |
| `/docs/code-to-emulate-in-real-project.html` | Código que sirve como referencia para un proyecto real |

También existe una guía Markdown en:

```txt
docs/code-to-emulate-in-real-project.md
```

Esa guía explica qué partes conviene emular en un proyecto real y qué partes son solo de demostración.

---

# Roadmap posible

Ideas para futuras mejoras:

```txt
Publicar imagen Docker
Tests automatizados
GitHub Actions
Exportar/importar estado
Más planes configurables
Configurar planes por archivo
Mejorar dashboard visual
Agregar filtros por estado
Agregar detalle modal para suscripciones y pagos
Agregar comparación visual con datos del proveedor
Modo multiusuario educativo
```

---

# Licencia

Este proyecto usa licencia MIT.

Ver:

```txt
LICENSE
```