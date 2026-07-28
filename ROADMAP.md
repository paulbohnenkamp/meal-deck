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

The current MVP uses a synchronous `POST /api/extractions` endpoint and saves inventory only through the existing confirmed meal endpoint.

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
