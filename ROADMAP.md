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

- Upload box-front and nutrition-sheet photos as a capture set
- Backend image preprocessing and orientation correction
- OCR/vision provider abstraction
- Structured extraction for name, description, servings, calories, carbs, protein, fat, and sodium
- Confidence score per field
- Mandatory review screen before inventory mutation
- Retain original extraction evidence for correction and debugging
- Never expose provider credentials to the client

Suggested contract:

```text
POST /api/extractions
GET  /api/extractions/{id}
POST /api/extractions/{id}/confirm
```

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
