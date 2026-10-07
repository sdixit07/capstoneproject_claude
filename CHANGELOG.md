# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `GET /api/products/{id}` endpoint returning the full details of a single product (`id`, `name`, `description`, `imageUrl`, `price`, `category`) with `200`, `404` for a well-formed id that does not exist, and `400` for a non-numeric or out-of-range id, so the product details page can show full information before purchasing (EPMCDMETST-67216)
- `ProductService.getProductById(Long)`, which raises a `404 ResponseStatusException` with the message `Product not found with id <id>` when the product is absent (EPMCDMETST-67216)
- Backend tests for the new endpoint: `ProductServiceTest`, `ProductByIdIntegrationTest` and `ProductByIdErrorBodyIntegrationTest` covering the 200/404/400 paths, the `0`/`-1`/out-of-range edge cases, the unchanged category path and the configured CORS origin (EPMCDMETST-67216)
- Backend tests closing pre-existing coverage gaps so the module clears the 85% line-coverage gate: `CategoryControllerIntegrationTest`, `CategoryServiceTest` and `CategoryAndProductModelTest` (EPMCDMETST-67216)
- `jacoco-maven-plugin` (`prepare-agent` and `report`) so `mvnw test` produces a coverage report under `target/site/jacoco` (EPMCDMETST-67216)
- README documentation for the product endpoints, the new product details endpoint with its response and error bodies, Docker-free local run commands for back-end and front-end, and the test commands (EPMCDMETST-67216)

### Changed
- `server.error.include-message=always` is now set in `application.properties`, so the default error body carries a `message` field and the front-end can distinguish a missing product from a malformed id. This applies to every endpoint and is intended for local development only (EPMCDMETST-67216)
