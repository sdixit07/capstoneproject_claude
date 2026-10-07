# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- `ProductService.getProductById(Long id)` returning the product or throwing `ProductNotFoundException` when absent (EPMCDMETST-67217)
- Unchecked `ProductNotFoundException` mapped to HTTP 404 via `@ResponseStatus`, with the id in its message (EPMCDMETST-67217)
- JaCoCo Maven plugin producing a coverage report during `verify` (EPMCDMETST-67217)
- Unit tests for services, controllers, entities and `DataSeeder`; 20 tests, 98.4% instruction coverage (EPMCDMETST-67217)
