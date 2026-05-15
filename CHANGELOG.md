# Changelog

All notable changes to this project will be documented in this file.

This project follows a simple semantic versioning style:

- `MAJOR`: breaking changes.
- `MINOR`: new features.
- `PATCH`: fixes, documentation or small improvements.

---

## 1.2.0 - 2026-05-15

### Added

- Added separated subscription creation and payment flow.
- Added `POST /api/subscriptions` to create an internal subscription and provider preapproval without paying immediately.
- Added `POST /api/subscriptions/{id}/pay` to pay an existing subscription with a fictitious card.

### Changed

- Updated StreamBox Demo UI to replace the combined `Suscribirme y pagar` action with two actions: `Suscribirme` and `Pagar`.
- Updated help content and documentation to describe the separated subscription and payment flow.
- Kept `POST /api/subscriptions/start` as a legacy combined endpoint for compatibility.

---

## 1.1.0 - 2026-05-14

### Added

- Added integrated `Docs` menu to StreamBox Demo UI.
- Added static HTML documentation section under `/docs/`.
- Added documentation index page.
- Added quick start guide.
- Added Docker and configuration guide.
- Added webhooks and internal state guide.
- Added troubleshooting guide.
- Added testing scenarios guide.
- Added security and limitations guide.
- Added internal architecture guide.
- Added real payment provider migration guide.
- Added API reference guide.
- Added legal responsibilities, privacy and fraud prevention guide.
- Added payment code walkthrough documentation.
- Added navigable documentation pages for using StreamBox Demo without opening the project in an IDE.

### Changed

- Expanded project documentation for Docker-based usage.
- Improved documentation around the relationship between StreamBox Demo and Mock Payment Service.
- Improved documentation around what code can be used as a reference for real projects.

### Notes

This release focuses on documentation and developer experience.

StreamBox Demo can now be used as a Docker-based educational application while still exposing detailed documentation about its payment integration flow directly from the browser.

---

## 1.0.0 - Initial release

### Added

- Initial stable release of Subscription Demo App.
- StreamBox Demo UI served from Spring Boot.
- Educational demo showing the application-side of a payment integration.
- Integration with `mock-payment-service`.
- Hardcoded subscription plans.
- Internal subscription state.
- Internal payment state.
- Internal event log.
- Received webhook registry.
- Initial subscription creation flow.
- Fake card payment simulation through Mock Payment Service.
- Recurring charge simulation.
- Subscription plan change flow.
- Subscription cancellation flow.
- Webhook receiver endpoint for Mock Payment Service events.
- Webhook signature validation using HMAC SHA-256.
- Webhook processing for payment and preapproval events.
- Provider resource lookup after receiving webhooks.
- Mapping between provider events and internal application state.
- Integration configuration endpoint.
- Integration configuration panel in the UI.
- Demo state endpoint.
- Demo state reset endpoint.
- Memory and file storage modes.
- Dockerfile.
- Docker Compose setup.
- Configurable Docker host port through `DEMO_APP_HOST_PORT`.
- Configurable Docker Compose environment variables for:
  - `MOCK_PAYMENT_BASE_URL`
  - `MOCK_PAYMENT_WEBHOOK_SECRET`
  - `DEMO_APP_PUBLIC_BASE_URL`
  - `DEMO_APP_WEBHOOK_BASE_URL`
  - `DEMO_APP_STORAGE_TYPE`
  - `DEMO_APP_STORAGE_FILE_PATH`
- Shared Docker network support for running with Mock Payment Service.
- `.env.example` file.
- Main project `README.md`.
- Documentation explaining which parts of the code can be reused as patterns in a real project.
- Integrated help modal in the UI.
- MIT license.

### Notes

This application is a local educational demo.

It does not process real payments, real cards or real subscriptions.

It is designed to run together with `mock-payment-service`.

The container listens internally on port `8080`.

The host port can be changed with:

```bash
DEMO_APP_HOST_PORT=8085 docker compose up --build
```

Other containers in the same Docker network should continue using:

```txt
http://subscription-demo-app:8080
```

StreamBox Demo should call Mock Payment Service using:

```txt
http://mock-payment-service:9090
```

### Disclaimer

This project is not affiliated with Mercado Pago, Stripe, PayPal or any real payment provider.

It is a local demo application for development and learning purposes only.