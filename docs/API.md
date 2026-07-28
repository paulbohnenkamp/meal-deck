# API overview

Base URL: `http://localhost:8080`

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/meals` | List inventory |
| POST | `/api/meals` | Add one meal; matching normalized names merge |
| PUT | `/api/meals/{id}` | Replace editable meal fields |
| DELETE | `/api/meals/{id}` | Delete an inventory type |
| POST | `/api/meals/{id}/consume?avoidDays=7` | Eat a chosen meal |
| POST | `/api/picks/preview?avoidDays=7&allowRecent=false` | Preview a draw without changing inventory |
| GET | `/api/history` | List dinner history |
| POST | `/api/history/{id}/undo` | Restore one meal and remove history entry |
| GET | `/api/dashboard?avoidDays=7` | Inventory and eligibility counts |
| POST | `/api/uploads` | Upload a meal photo as multipart field `file` |
| POST | `/api/extractions` | Extract reviewable details from multipart fields `front` and `back` |

## Meal-card capture example

The values below are illustrative. The actual values come from the photographed meal card and remain editable during review.

### 1. Extraction response

`POST /api/extractions` accepts the meal-card front as multipart field `front` and the cooking-guide back as multipart field `back`.

```json
{
  "name": "Teriyaki Salmon with White Rice and Broccoli",
  "description": "Teriyaki-glazed salmon served with white rice and broccoli",
  "category": "Seafood",
  "caloriesPerServing": 510,
  "carbsPerServing": 42,
  "proteinPerServing": 31,
  "fatPerServing": 18,
  "sodiumMgPerServing": 850
}
```

This response only prefills the review screen. It does not change inventory.

### 2. Reviewed add-meal request

After review, the client uploads both photos and sends the confirmed values to `POST /api/meals`. Each capture adds one meal; matching normalized names consolidate in inventory.

```json
{
  "name": "Teriyaki Salmon with White Rice and Broccoli",
  "description": "Teriyaki-glazed salmon served with white rice and broccoli",
  "category": "Seafood",
  "quantity": 1,
  "caloriesPerServing": 510,
  "carbsPerServing": 42,
  "proteinPerServing": 31,
  "fatPerServing": 18,
  "sodiumMgPerServing": 850,
  "imageUrl": "http://localhost:8080/uploads/teriyaki-salmon-front.jpg",
  "cookingGuideImageUrl": "http://localhost:8080/uploads/teriyaki-salmon-back.jpg",
  "source": "PHOTO"
}
```

### 3. Saved meal response

The saved response includes server-owned identity, fixed serving count, consolidated quantity, and timestamps.

```json
{
  "id": "ef63bc2d-cdf7-43ec-b9cb-ab74e554dc82",
  "name": "Teriyaki Salmon with White Rice and Broccoli",
  "description": "Teriyaki-glazed salmon served with white rice and broccoli",
  "category": "Seafood",
  "quantity": 1,
  "servings": 2,
  "caloriesPerServing": 510,
  "carbsPerServing": 42,
  "proteinPerServing": 31,
  "fatPerServing": 18,
  "sodiumMgPerServing": 850,
  "imageUrl": "http://localhost:8080/uploads/teriyaki-salmon-front.jpg",
  "cookingGuideImageUrl": "http://localhost:8080/uploads/teriyaki-salmon-back.jpg",
  "source": "PHOTO",
  "createdAt": "2026-07-28T21:00:00Z",
  "updatedAt": "2026-07-28T21:00:00Z"
}
```
