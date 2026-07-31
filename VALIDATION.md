# Validation performed

Validated on July 30, 2026:

- `cd mobile && npm test` — 7 passed, 0 failed
- `cd mobile && npm run typecheck` — passed
- `cd mobile && npx expo export --platform web` — passed
- `cd backend && ../mvnw test` with Java 21 — 14 passed, 0 failures, 0 errors
- `cd backend && ../mvnw javadoc:javadoc` — passed
- `git diff --check` — passed

Backend coverage includes random eligibility, atomic consumption and undo, cooking-code and
identifier persistence, barcode/QR decoding, immutable template revisions, packing-slip
classification, shipment rollback, and duplicate-order idempotency.

Generated build directories are intentionally excluded from the downloadable source ZIP.
