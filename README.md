# MealDeck

A working MVP for photographing prepared meal cards, tracking freezer inventory, counting nutrition, and randomly choosing dinner without repeating a meal eaten in the previous seven days.

## What is included

- Universal Expo app for iOS and web
- Runs immediately in local/offline mode with on-device persistence
- Optional Spring Boot 4.1 / Java 21 backend with H2 persistence and photo uploads
- Capture or select the meal-card front and cooking-guide back in a guided sequence
- Store and display nutrition per serving
- Consolidate duplicate meal names into one inventory record
- Random dinner draw that excludes meals eaten in the last seven days
- Inventory decrement only after the user accepts a draw
- Manual “Eat this” flow
- Dinner history and undo
- Sample meals so the first launch is not empty
- Backend service tests and mobile TypeScript checks

## Repository layout

```text
meal-deck/
├── mobile/       Expo / React Native app (iOS + web)
├── backend/      Spring Boot REST API
├── docs/         Architecture and API notes
├── AGENTS.md     Guidance for Codex and coding agents
└── ROADMAP.md    Suggested milestones after the MVP
```

### Spring backend packages

The Java backend uses a layered package layout so each Spring responsibility is easy to identify:

```text
app.mealdeck
├── config/       Spring and application configuration
├── controller/   HTTP endpoints and request routing
├── dto/          Separate request and response API records
├── entity/       JPA persistence entities
├── exception/    Domain exceptions and HTTP error translation
├── mapper/       Entity-to-response DTO conversion
├── repository/   Spring Data JPA repositories
└── service/      Inventory and dinner-selection business rules
```

Dependencies flow inward from controller to service, then to repositories and mappers. Entities do not create API DTOs themselves, and the service throws domain exceptions rather than coupling business rules to HTTP status classes.

## Fastest way to try it

The app defaults to **local mode**, so no backend is required.

```bash
cd mobile
npm install
npm run web
```

For iOS on a Mac:

```bash
cd mobile
npm install
npm run ios
```

You can also run `npm start` and use an Expo development build. SDK 57 projects may not run in the public App Store version of Expo Go; a simulator or Expo development build is the reliable path.

## Run with the Spring Boot backend

### 1. Start the API

Java 21 is required.

```bash
cd backend
../mvnw spring-boot:run
```

The API runs at `http://localhost:8080`. Data and uploaded photos are stored under `backend/data/`.

To enable automatic meal-card extraction, set `OPENAI_API_KEY` in the backend environment before starting Spring Boot. The optional `OPENAI_MODEL` setting defaults to `gpt-5.6-sol`. The key is used only by the backend and must never be added to the Expo environment.

Alternatively:

```bash
docker compose up --build
```

### 2. Point the app at it

```bash
cd mobile
cp .env.example .env
npm run ios
```

Use `npm start` instead when you want to start Expo first and choose iOS, Android, or web from its developer menu.

For web or the iOS simulator, use:

```dotenv
EXPO_PUBLIC_API_URL=http://localhost:8080
```

For a physical iPhone, use your Mac’s LAN address, such as:

```dotenv
EXPO_PUBLIC_API_URL=http://192.168.1.25:8080
```

The phone and Mac must be on the same network. For a production build, use an HTTPS API URL.

## Verification commands

```bash
cd mobile
npm run typecheck
npx expo export --platform web

cd ../backend
../mvnw test
```

## Core behavior

### Duplicate meals

Names are normalized for comparison. Adding another “Chicken Alfredo” increases its quantity rather than creating a second row. Nutrition and front/back photo values supplied by the newest entry become the current values.

### Random draw

The strict draw:

1. Finds meal types with at least one available meal.
2. Excludes normalized meal names found in dinner history during the previous seven days.
3. Selects uniformly from the remaining meal types.
4. Shows the selection without changing inventory so the user can draw again or cancel.
5. After the user confirms a selection, decrements quantity by one and creates a history entry in the same transaction.

When every stocked meal is recent, the UI offers a relaxed draw that ignores the seven-day rule.

### Nutrition

Nutrition is stored and displayed **per serving**. Every prepared meal represents two servings, but MealDeck does not store or display derived whole-meal nutrition totals.

## Photo extraction

After the meal-card front and cooking-guide back are captured, the backend uses vision extraction to prefill the meal overview and per-serving nutrition. The user reviews and may correct the result before inventory changes. Extraction requires `OPENAI_API_KEY`; provider credentials never ship inside the iOS/web bundle.

## Production work still required

Before an App Store launch, add authentication, cloud object storage, a production database, privacy disclosures, analytics/crash reporting, extraction evaluation and resilience, accessibility review, EAS build configuration, and real-device testing. Also validate permission wording and the working product name.
