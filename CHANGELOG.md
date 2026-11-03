# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- `GET /api/products` accepts `page` (default 0) and `size` (default 10, maximum 100) query parameters and returns a paged envelope (EPMCDMETST-69858)
- `400` responses for negative or non-numeric `page` and out-of-range or non-numeric `size` (EPMCDMETST-69858)
- Seed data extended to at least 25 products so pagination spans several pages (EPMCDMETST-69858)
- Backend pagination tests and a Playwright API spec covering acceptance criteria AC-1 to AC-10 (EPMCDMETST-69858)

### Changed
- **Breaking:** `GET /api/products` now returns a paged envelope instead of a bare array; the current front end `App.jsx` needs a follow-up Story to consume it (EPMCDMETST-69858)
- Products are returned ordered by ascending id (EPMCDMETST-69858)
- Vitest excludes `tests/**` so Playwright specs are no longer collected by `npm test` (EPMCDMETST-69858)

### Fixed
- `npm test` no longer fails on the Playwright spec (known issue KI-1 resolved) (EPMCDMETST-69858)
- `docs/KNOWN-ISSUES.md` KI-4 corrected to the measured 3 failures (EPMCDMETST-69858)
