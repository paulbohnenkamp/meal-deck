# MealDeck Roadmap

## Milestone 1 — MVP hardening

- Edit all meal details from the inventory screen
- Better duplicate review when names are similar but not identical
- Empty-state onboarding and “replace sample data” action
- Configurable repeat window, defaulting to seven days
- Accessibility labels and VoiceOver testing
- EAS development and TestFlight build profiles
- API integration tests and mobile component tests

## Milestone 2 — Photo-to-meal extraction

### Working slice completed

- [x] Guided meal-card front and cooking-guide back capture
- [x] Explicit “Read meal card” consent before photos are uploaded
- [x] Backend-only OpenAI vision integration with structured extraction
- [x] Extract name, description, category, calories, carbs, protein, fat, and sodium
- [x] Keep nutrition values per serving
- [x] Mandatory editable review before inventory mutation
- [x] Store both photos with the confirmed meal
- [x] Keep provider credentials out of the Expo client
- [x] Preserve local inventory and dinner-selection behavior when no API is configured

### Remaining production hardening

- [ ] Make extraction an asynchronous job with polling and retry-safe IDs
- [ ] Add backend image preprocessing and orientation correction
- [ ] Add confidence and source evidence per extracted field
- [ ] Retain extraction evidence for correction, debugging, and evaluation
- [ ] Build a representative meal-card evaluation set
- [ ] Add provider timeouts, retry policy, rate limits, and cost telemetry
- [ ] Add a production fallback when extraction is unavailable

Target asynchronous contract:

```text
POST /api/extractions
GET  /api/extractions/{id}
POST /api/extractions/{id}/confirm
```

Meal-card extraction currently uses synchronous `POST /api/extractions`; the user then saves the
reviewed meal through `POST /api/meals`. Packing-slip review and atomic shipment confirmation use
their own endpoints.

### Suvie-assisted box intake

A Suvie delivery has several potentially useful identifiers:

1. The packing slip lists each item code, quantity, and meal description for the
   shipment.
2. The meal-card front repeats the cooking meal code and has a barcode near the
   bottom.
3. The cooking-guide back repeats the meal code that is needed when programming
   the appliance.
4. The card also has a QR code that may identify the meal or encode cooking
   instructions.

Treat these as separate identifiers until real samples prove that any two are
equivalent. Do not join records by meal name when an exact provider code is
available. The cooking meal code is operational information, not merely import
metadata, and must remain easy to find after the physical card is put away.

Completed delivery slices:

1. **Printed cooking code end to end.** Persist one confirmed
   `cookingMealCode`, extract candidates from both card photos, require review,
   support it in local and server modes, and show it prominently on inventory
   and dinner-selection surfaces.
2. **Machine-readable card identifiers.** Decode the front barcode and
   back QR code, retain their raw payloads with the source photos, and require
   explicit review without assuming either payload equals the printed cooking
   code.
3. **Reusable meal templates.** Index immutable, revisioned, reviewed
   definitions by cooking code, barcode, and QR payload so known meals can be
   added again without rescanning cards in either server or local mode.
4. **Packing-slip manifest intake.** Privately crop to the item table,
   extract review-only shipment rows, match them to templates, and separate
   known, changed, and unknown meals without changing inventory.
5. **Atomic shipment confirmation.** Require a fully resolved manifest
   and order ID, then update all consolidated inventory quantities and record
   shipment idempotency in one transaction in server or local mode.

Implemented intake flow:

1. Scan the packing slip's item table once to create a reviewable shipment
   manifest.
2. Match each row by provider and external meal code against previously
   confirmed meal templates.
3. For a known code, show the extracted description and active template match,
   retain the extracted quantity, and do not require another pair of card
   photos.
4. For an unknown or changed code, block shipment confirmation until the meal
   cards have been reviewed and saved as an active template.
5. Save newly reviewed card details as a reusable template, refresh the
   manifest match, and confirm all resolved boxes in one inventory transaction.
6. If a familiar code has materially different card text or nutrition, require
   review and create a new template revision instead of silently overwriting
   the old definition.
7. If the printed front code, printed back code, barcode result, and QR result
   disagree, block automatic confirmation and ask the user to choose the code
   shown by the appliance instructions.

This separates a reusable meal definition from a delivery and from current
inventory:

```text
MealTemplate
  provider
  cookingMealCode
  frontBarcodePayload
  backQrPayload
  normalizedName
  description and category
  per-serving nutrition
  front/back reference images
  revision, active, and verifiedAt

Shipment
  externalOrderId
  shippedAt
  totalBoxes
  confirmedAt

InventoryMeal
  normalizedName
  quantity of two-serving boxes
  reviewed definition copied from the active template
```

External codes act as aliases for duplicate consolidation: two codes may map to
the same normalized meal, but duplicate names still consolidate into one
inventory row. Nutrition copied into dinner history remains a snapshot even
when a template is revised later.

Current privacy behavior for packing-slip scanning:

- The label contains a recipient name, street address, order number, and
  shipment date. Treat the full image as sensitive personal data.
- The system image editor asks the user to crop to the item table before
  upload so the shipping address can remain on-device.
- The backend does not persist the submitted packing-slip image.
- Persist only the user-confirmed order ID, printed shipment date, total box
  count, and confirmation timestamp for idempotency.
- Provide an editable manifest review before changing inventory.

Completed meal-code behavior:

- Extract the printed cooking meal code from both front and back card photos.
- Decode the front barcode and back QR code separately and retain their exact
  reviewed payloads with the meal/template.
- Show the proposed cooking code as a dedicated required review field rather
  than hiding it in OCR evidence.
- Preserve leading zeroes, letters, punctuation, and capitalization; meal codes
  are identifiers and must never be parsed as numbers.
- Give conflicting front/back code candidates their own warning state.
- Keep cooking code separate from packing-slip item code until verified.
- Make the cooking code prominent on inventory cards, freezer previews, and
  random-draw previews.
- Provide a large, high-contrast, selectable cooking code.
- Keep the code available offline after a meal has been imported.

Follow-up backlog:

- [ ] Collect several card backs, QR payloads, and packing slips to determine
      which identifiers are stable across repeat orders
- [ ] Add live QR/barcode camera scanning with an accessible manual-code
      fallback; current decoding operates on submitted card photos
- [ ] Add confidence and per-field source evidence for printed cooking codes
- [ ] Record whether the confirmed cooking code came from front text, back text,
      or a manual correction
- [ ] Add a one-tap copy action and optional “Show cooking card” detail action
- [ ] Add automated on-device packing-slip table detection; current cropping is
      user-controlled
- [ ] Add a guided resolution action from each changed or unknown manifest row
      directly into card capture, then refresh the manifest match
- [ ] Persist reviewed shipment line snapshots if shipment audit/history becomes
      a product requirement; current persistence records idempotency metadata
- [ ] Test partial shipments, duplicate rows, unreadable codes, four-serving
      packages, and a known code whose nutrition has changed
- [ ] Investigate USDA FoodData Central and Open Food Facts as authorized
      barcode fallbacks
- [ ] Request written permission and a supported catalog contract before using
      Suvie's undocumented website APIs, meal text, or images

Useful browse and inventory UI patterns:

- Horizontal filters for All, New, protein/cuisine, vegetarian, and carb range
- Photo-forward cards with name and subtitle, while keeping carbs and box count
  visible without opening details
- A prominent, high-contrast cooking meal code on every cooking-oriented meal
  surface
- Badges for new template, unknown code, changed card, recently eaten, and
  strict-draw eligibility
- A detail sheet for cooking instructions and full nutrition
- Separate “Browse/import” and “In freezer” states so discovery content is not
  mistaken for owned inventory

## Milestone 3 — Household sync

- Sign in with Apple
- PostgreSQL and schema migrations
- Object storage for meal photos
- Household membership and invitations
- Optimistic local cache with background sync
- Conflict handling for simultaneous inventory changes
- Audit log for add, consume, undo, and delete

## Milestone 4 — Smarter dinner decisions

- Filters for carb range, calories, protein, prep time, and category
- Weighted randomization based on quantity and last-eaten date
- “Pick three” choice mode
- Favorite and avoid ratings
- Planned dinner calendar
- Side-dish suggestions based on stored nutrition

## Milestone 5 — App Store readiness

- Final product naming and trademark review
- Privacy policy and support URL
- Permission, data-retention, and account-deletion flows
- Crash reporting and privacy-conscious analytics
- App Store screenshots and metadata
- TestFlight household beta
- Security review, rate limiting, backups, and production monitoring
