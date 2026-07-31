# Architecture

## Mobile

The Expo client uses one responsive React Native codebase for iOS and web. A small tab shell keeps the MVP dependency-light. `data.ts` is the boundary between UI and storage:

- No `EXPO_PUBLIC_API_URL`: `localStore.ts` persists JSON with AsyncStorage.
- API URL configured: requests use the Spring Boot API and photo-upload endpoint.

The same domain operations exist in both paths: list, add, update, delete, consume, random pick,
history, undo, dashboard, reusable-template lookup, and atomic shipment confirmation. Multimodal
meal-card and packing-slip extraction require the backend because provider credentials never enter
the Expo bundle.

Local state stores inventory, history, active and superseded template revisions, and confirmed
order IDs in one serialized AsyncStorage document. Mutations run through an in-process queue so a
local shipment confirmation cannot partially overlap another operation.

## Backend

The backend uses a conventional controller/service/repository structure.

```text
MealDeckController ───────→ MealDeckService ──────→ meal + history repositories
                           (@Transactional)

MealExtractionController → OpenAiMealExtractionService
                           ├─ Spring AI structured multimodal extraction
                           └─ ZXing barcode/QR decoding

PackingSlipController ───→ PackingSlipExtractionService
                           └─ PackingSlipMatcher ─→ active meal templates

ShipmentController ──────→ ShipmentService ──────→ meals + templates + shipments
                           (@Transactional)
```

The random draw and consumption write occur in one service transaction. History stores name and nutrition snapshots rather than depending only on the current meal record.

`MealTemplate` is independent of inventory quantity. Confirmed identifier-bearing meal definitions
create immutable revisions; only the latest matching revision remains active. Exact cooking-code,
barcode-payload, and QR-payload lookups can therefore reuse a definition after inventory reaches
zero or is deleted.

Packing-slip extraction is review-only. The client crops to the item table before upload, and the
backend does not persist the submitted slip image. Rows are conservatively classified:

- `KNOWN`: item code resolves to an active template and the normalized description matches.
- `CHANGED`: item code resolves, but the printed description differs.
- `UNKNOWN`: no active template resolves the item code.

`ShipmentService` rejects unresolved or inactive templates before writing anything. It then
consolidates normalized names, increments all box quantities, and saves a unique external order ID
in one transaction. The local store mirrors the same prevalidation and idempotency behavior.

## AI and image boundaries

- OpenAI credentials are backend-only.
- Spring AI requests use provider-native structured output.
- OpenAI response storage is disabled for meal-card and packing-slip extraction.
- ZXing decodes machine-readable identifiers from submitted card bytes locally on the backend.
- Uploaded meal-card photos may be retained with a confirmed meal.
- Packing-slip images are not stored; only the confirmed order ID and shipment metadata persist.

## Production evolution

Replace H2 with PostgreSQL and local upload storage with S3-compatible object storage. Add Flyway
migrations before shared environments. Authentication should establish a household scope on every
meal, template, shipment, and history query.
