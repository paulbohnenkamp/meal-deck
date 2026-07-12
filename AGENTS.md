# AGENTS.md — MealDeck

## Product intent

MealDeck helps a two-person household keep an accurate freezer inventory of prepared meals and decide dinner without repeating a meal eaten in the last seven days. Carb visibility is a first-class requirement.

## Non-negotiable behavior

1. Every meal box represents exactly two servings in the MVP.
2. Nutrition fields are stored per serving; whole-box values are derived.
3. A completed random draw decrements inventory and records history atomically.
4. Strict random draws exclude meal names eaten within `avoidDays` (default seven).
5. Duplicate names consolidate by normalized name.
6. Undoing history restores one box.
7. Never embed OCR/LLM provider secrets in the Expo client.
8. The local data store must remain functional when no API URL is configured.

## Architecture

- `mobile/`: Expo SDK 57, React 19, React Native 0.86, TypeScript.
- `backend/`: Spring Boot 4.1, Java 21, Spring MVC, JPA, H2.
- `mobile/src/data.ts`: selects server mode only when `EXPO_PUBLIC_API_URL` exists.
- `mobile/src/localStore.ts`: local/offline behavior and must mirror server semantics.
- `backend/.../MealDeckService.java`: owns inventory/history transaction rules.

## Development rules

- Keep server and local implementations behaviorally aligned.
- Add tests for domain-rule changes, especially random eligibility and inventory mutation.
- Treat photo extraction as an asynchronous backend capability with a human review step.
- Preserve snapshot nutrition in history so later meal edits do not rewrite past meals.
- Do not remove local mode when adding authentication or cloud sync.
- Avoid introducing navigation/state libraries until the UI complexity justifies them.
- Use accessible labels, adequate hit targets, and dynamic text-safe layouts.

## Commands

```bash
cd mobile && npm run typecheck
cd mobile && npx expo export --platform web
cd backend && ../mvnw test
```

## Definition of done

A change is done when TypeScript checks pass, the web bundle exports, backend tests pass, documentation reflects any setup change, and both local and server modes preserve the core product rules.
