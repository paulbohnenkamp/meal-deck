# Validation performed

Validated on July 10, 2026:

- `mobile/npm run typecheck` — passed
- `mobile/npx expo export --platform web` — passed
- `backend/mvn test` with Java 21 and Maven 3.9.16 — passed
- Backend tests: 2 run, 0 failures, 0 errors
- `backend/mvn package` — produced an executable Spring Boot JAR
- API smoke test — application started, sample inventory loaded, `/api/meals` returned data, and `/api/picks/random` decremented inventory and created history

Generated build directories are intentionally excluded from the downloadable source ZIP.
