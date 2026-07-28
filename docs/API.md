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

Example meal body:

```json
{
  "name": "Chicken Tikka Masala",
  "description": "Chicken with basmati rice",
  "category": "Chicken",
  "quantity": 1,
  "caloriesPerServing": 510,
  "carbsPerServing": 42,
  "proteinPerServing": 31,
  "fatPerServing": 18,
  "sodiumMgPerServing": 850,
  "imageUrl": "/uploads/example.jpg",
  "cookingGuideImageUrl": "/uploads/example-back.jpg",
  "source": "PHOTO"
}
```
