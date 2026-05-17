# Contrato entre StreamBox Demo y Mock Payment Service

Este documento resume el contrato minimo entre `subscription-demo-app` y `mock-payment-service`.

## Responsabilidades

StreamBox Demo representa la aplicacion principal:

- Crea y guarda suscripciones internas.
- Llama al mock para crear preapprovals y pagos.
- Guarda `providerSubscriptionId` y `providerExternalReference`.
- Recibe webhooks firmados.
- Valida firma y consulta el recurso actualizado antes de actualizar su estado interno.

Mock Payment Service representa la pasarela simulada:

- Crea preapprovals.
- Procesa pagos con tarjetas ficticias.
- Cambia estados de preapproval.
- Emite webhooks firmados al `notification_url`.
- Permite simular respuestas lentas o perdidas despues de crear la preapproval.

## Campos principales

| Campo | Dueño | Uso |
|---|---|---|
| `demo-subscription-{n}` | StreamBox | ID interno de la suscripcion. |
| `mock-preapproval-{n}` | Mock | ID externo de la suscripcion en el proveedor simulado. |
| `mock-payment-{n}` | Mock | ID externo del pago. |
| `external_reference` | StreamBox | Referencia idempotente para reconciliar una creacion dudosa. |
| `notification_url` | StreamBox | Endpoint donde el mock debe enviar webhooks. |
| `x-request-id` | Mock | ID tecnico para rastrear la entrega del webhook. |
| `x-signature` | Mock | Firma HMAC del webhook. |

## Flujo esperado

1. StreamBox crea una suscripcion interna en `CREATING`.
2. StreamBox llama a `POST /preapproval`.
3. Mock guarda la preapproval y responde con `mock-preapproval-{n}`.
4. StreamBox guarda el ID externo y pasa la suscripcion a `PENDING`.
5. StreamBox solicita el pago o una accion sobre la suscripcion.
6. Mock procesa la operacion y emite webhook.
7. StreamBox valida el webhook, consulta `GET /payment/{id}` o `GET /preapproval/{id}` y actualiza su estado interno.

## Resiliencia

Si StreamBox no puede confirmar la creacion de la preapproval, deja la suscripcion en `RECONCILIATION_NEEDED`.

La reconciliacion usa:

```txt
GET /preapproval/by-external-reference/{externalReference}
```

Esto permite recuperar una preapproval que el mock si creo, aunque la respuesta HTTP se haya perdido.
