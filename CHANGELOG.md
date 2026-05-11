# Changelog

All notable changes to this project will be documented in this file.

This project follows a simple semantic versioning style:

- `MAJOR`: breaking changes.
- `MINOR`: new features.
- `PATCH`: fixes, documentation or small improvements.

---

## 1.0.0 - Unreleased

### Added

- Initial StreamBox Demo application.
- Spring Boot backend serving a static UI.
- Hardcoded subscription plans.
- Integration with Mock Payment Service.
- Initial subscription creation flow.
- Fake card payment simulation through Mock Payment Service.
- Internal subscription state.
- Internal payment state.
- Internal event log.
- Received webhook registry.
- Webhook signature validation.
- Webhook processing for payment and preapproval events.
- Support for recurring charge simulation.
- Support for subscription plan change.
- Support for subscription cancellation.
- Memory and file storage modes.
- Demo state reset endpoint.
- Integration configuration endpoint.
- Integration configuration panel in the UI.
- Dockerfile.
- Docker Compose setup.
- Configurable host port through `DEMO_APP_HOST_PORT`.
- Shared Docker network support for communicating with Mock Payment Service.
- Integrated help modal in the UI.
- Documentation explaining which parts of the code can be reused as patterns in a real project.
- Main project README.
- Environment example file.

### Notes

This application is a local educational demo. It does not process real payments and should not be used as a production billing system.

It is intended to run together with `mock-payment-service`.