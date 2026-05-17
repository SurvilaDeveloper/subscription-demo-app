# Checklist de release de StreamBox Demo

Usar esta lista antes de publicar una nueva version de `subscription-demo-app`.

## Version y build

- Confirmar que `pom.xml` tenga la version de release `1.3.0`, sin `-SNAPSHOT`.
- Ejecutar `mvn test`.
- Levantar StreamBox y Mock Payment Service juntos.
- Abrir StreamBox en `http://localhost:8080/` o en el puerto publicado configurado.

## Smoke test funcional

1. Resetear el estado de StreamBox.
2. Resetear el estado del mock.
3. Crear una suscripcion desde StreamBox.
4. Verificar que la suscripcion quede `PENDING` y tenga `providerSubscriptionId`.
5. Pagar la suscripcion con la tarjeta ficticia aprobada `4111111111111111`.
6. Verificar que la suscripcion quede `ACTIVE`.
7. Verificar que StreamBox muestre el pago interno.
8. Verificar que la seccion "Webhooks recibidos por StreamBox" muestre el webhook.
9. Abrir "Ver webhook" y confirmar que el payload sea legible.
10. Confirmar que los errores visibles esten en espanol y no expongan stack traces ni JSON crudo.

## Smoke test de resiliencia

1. En Mock Payment Studio, configurar la proxima creacion de preapproval como "Crea pero pierde respuesta".
2. Crear una suscripcion desde StreamBox.
3. Confirmar que StreamBox deje la suscripcion en `RECONCILIATION_NEEDED`.
4. Usar "Reconciliar".
5. Confirmar que StreamBox encuentre la preapproval por `external_reference` y actualice el estado.

## Contrato con el mock

- `MOCK_PAYMENT_BASE_URL` debe apuntar al mock desde el punto de vista de StreamBox.
- `DEMO_APP_WEBHOOK_BASE_URL` debe ser alcanzable desde el mock.
- `MOCK_PAYMENT_WEBHOOK_SECRET` debe coincidir en ambos proyectos.
- StreamBox debe crear preapprovals con `external_reference = demo_subscription_id={subscriptionId}`.
- StreamBox no debe confiar solo en el webhook: despues de recibirlo, consulta el recurso actualizado en el mock.
