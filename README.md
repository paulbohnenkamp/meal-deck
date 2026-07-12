# MealDeck

A working MVP for photographing prepared meal boxes or cards, tracking freezer inventory, counting nutrition, and randomly choosing dinner without repeating a meal eaten in the previous seven days.

## What is included

- Universal Expo app for iOS and web
- Runs immediately in local/offline mode with on-device persistence
- Optional Spring Boot 4.1 / Java 21 backend with H2 persistence and photo uploads
- Take or select a meal-card photo
- Store nutrition per serving and calculate whole-box carbs (two servings)
- Consolidate duplicate meal names into one inventory record
- Random dinner draw that excludes meals eaten in the last seven days
- Automatic inventory decrement after a draw
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

Alternatively:

```bash
docker compose up --build
```

### 2. Point the app at it

```bash
cd mobile
cp .env.example .env
npm start
```

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

Names are normalized for comparison. Adding another “Chicken Alfredo” increases its quantity rather than creating a second row. Nutrition and photo values supplied by the newest entry become the current values.

### Random draw

The strict draw:

1. Finds meal types with at least one box.
2. Excludes normalized meal names found in dinner history during the previous seven days.
3. Selects uniformly from the remaining meal types.
4. Decrements quantity by one and creates a history entry in the same transaction.

When every stocked meal is recent, the UI offers a relaxed draw that ignores the seven-day rule.

### Nutrition

Nutrition is stored **per serving**, with two servings fixed for each meal box. The app emphasizes carbs per serving and whole-box carbs for carb counting.

## MVP boundary: photo extraction

The MVP captures and stores the meal-card photo but asks the user to verify the meal name and nutrition manually. Automated extraction should be implemented in the backend so model credentials never ship inside the iOS/web bundle. See `ROADMAP.md` for the proposed OCR and review flow.

## Production work still required

Before an App Store launch, add authentication, cloud object storage, a production database, privacy disclosures, analytics/crash reporting, automated OCR, accessibility review, EAS build configuration, and real-device testing. Also validate permission wording and the working product name.
