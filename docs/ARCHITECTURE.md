# Architecture

## Mobile

The Expo client uses one responsive React Native codebase for iOS and web. A small tab shell keeps the MVP dependency-light. `data.ts` is the boundary between UI and storage:

- No `EXPO_PUBLIC_API_URL`: `localStore.ts` persists JSON with AsyncStorage.
- API URL configured: requests use the Spring Boot API and photo-upload endpoint.

The same domain operations exist in both paths: list, add, update, delete, consume, random pick, history, undo, and dashboard.

## Backend

The backend uses a conventional controller/service/repository structure.

```text
HTTP controller
    ↓
MealDeckService (@Transactional)
    ↓
MealRepository + MealHistoryRepository
    ↓
H2 file database
```

The random draw and consumption write occur in one service transaction. History stores name and nutrition snapshots rather than depending only on the current meal record.

## Production evolution

Replace H2 with PostgreSQL and local upload storage with S3-compatible object storage. Add Flyway migrations before shared environments. Authentication should establish a household scope on every meal and history query.
