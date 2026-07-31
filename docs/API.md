# API overview

Base URL: `http://localhost:8080`

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/meals` | List inventory |
| POST | `/api/meals` | Add one meal; matching normalized names merge |
| PUT | `/api/meals/{id}` | Replace editable meal fields |
| DELETE | `/api/meals/{id}` | Delete an inventory type |
| POST | `/api/meals/{id}/consume?avoidDays=7` | Eat a chosen meal |
| POST | `/api/picks/random?avoidDays=7&allowRecent=false` | Choose and consume in one request |
| POST | `/api/picks/preview?avoidDays=7&allowRecent=false` | Preview a draw without changing inventory |
| GET | `/api/history` | List dinner history |
| POST | `/api/history/{id}/undo` | Restore one meal and remove history entry |
| GET | `/api/dashboard?avoidDays=7` | Inventory and eligibility counts |
| POST | `/api/uploads` | Upload a meal photo as multipart field `file` |
| POST | `/api/extractions` | Extract reviewable details from multipart fields `front` and `back` |
| GET | `/api/meal-templates/lookup?identifier=...` | Find an active reviewed template by exact identifier |
| POST | `/api/packing-slips/extract` | Extract and classify a cropped multipart field `slip` |
| POST | `/api/shipments/confirm` | Atomically apply a fully resolved shipment |

## Meal-card capture example

The values below are illustrative. The actual values come from the photographed meal card and remain editable during review.

### 1. Extraction response

`POST /api/extractions` accepts the meal-card front as multipart field `front` and the cooking-guide back as multipart field `back`.

```json
{
  "name": "Teriyaki Salmon with White Rice and Broccoli",
  "description": "Teriyaki-glazed salmon served with white rice and broccoli",
  "category": "Seafood",
  "frontCookingMealCode": "012-A",
  "backCookingMealCode": "012-A",
  "cookingMealCode": "012-A",
  "frontBarcodePayload": "310012345678",
  "backQrPayload": "https://suvie.com/m/012-A",
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
  "cookingMealCode": "012-A",
  "frontBarcodePayload": "310012345678",
  "backQrPayload": "https://suvie.com/m/012-A",
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
  "cookingMealCode": "012-A",
  "frontBarcodePayload": "310012345678",
  "backQrPayload": "https://suvie.com/m/012-A",
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

## Reusing a reviewed template

`GET /api/meal-templates/lookup?identifier=310012345678` accepts an exact cooking code,
front-barcode payload, or back-QR payload:

```json
{
  "id": "31954aaa-397c-4bd1-bdfc-a9f168fca09e",
  "provider": "SUVIE",
  "revision": 1,
  "cookingMealCode": "012-A",
  "frontBarcodePayload": "310012345678",
  "backQrPayload": "https://suvie.com/m/012-A",
  "name": "Teriyaki Salmon with White Rice and Broccoli",
  "description": "Teriyaki-glazed salmon served with white rice and broccoli",
  "category": "Seafood",
  "caloriesPerServing": 510,
  "carbsPerServing": 42,
  "proteinPerServing": 31,
  "fatPerServing": 18,
  "sodiumMgPerServing": 850,
  "imageUrl": "http://localhost:8080/uploads/teriyaki-salmon-front.jpg",
  "cookingGuideImageUrl": "http://localhost:8080/uploads/teriyaki-salmon-back.jpg",
  "verifiedAt": "2026-07-30T21:00:00Z"
}
```

Confirmed changes create a new immutable revision and deactivate the prior revision. Lookup returns
the active revision. Absence returns `404`; ambiguous aliases return `409`.

## Packing-slip review

`POST /api/packing-slips/extract` accepts a cropped item-table image as multipart field `slip`.
The image is not persisted and this endpoint does not change inventory. The nested template and
meal objects in the examples below are abbreviated; actual responses use the complete DTOs
documented above.

```json
{
  "orderId": "R2533747606",
  "shippedAt": "7/8/2026",
  "lines": [
    {
      "itemCode": "3C",
      "quantity": 1,
      "description": "Garlic & Herb Cheesy Breadsticks",
      "status": "KNOWN",
      "template": {
        "id": "31954aaa-397c-4bd1-bdfc-a9f168fca09e",
        "provider": "SUVIE",
        "revision": 1,
        "cookingMealCode": "3C",
        "name": "Garlic & Herb Cheesy Breadsticks"
      }
    },
    {
      "itemCode": "F99",
      "quantity": 1,
      "description": "Sweet Thai Chili Crab Cakes",
      "status": "UNKNOWN",
      "template": null
    }
  ]
}
```

Descriptions are normalized only for conservative classification. `CHANGED` means the item code
resolved but the printed description did not match the active template name.

## Atomic shipment confirmation

Only fully reviewed, active template rows should be sent to `POST /api/shipments/confirm`:

```json
{
  "orderId": "R2533747606",
  "shippedAt": "7/8/2026",
  "lines": [
    {
      "templateId": "31954aaa-397c-4bd1-bdfc-a9f168fca09e",
      "itemCode": "3C",
      "quantity": 1
    }
  ]
}
```

Success returns `201 Created`:

```json
{
  "orderId": "R2533747606",
  "totalBoxes": 1,
  "meals": [
    {
      "id": "ef63bc2d-cdf7-43ec-b9cb-ab74e554dc82",
      "name": "Garlic & Herb Cheesy Breadsticks",
      "quantity": 1,
      "servings": 2,
      "source": "SHIPMENT"
    }
  ],
  "confirmedAt": "2026-07-30T21:10:00Z"
}
```

The service prevalidates every template, item-code association, and quantity before mutation.
Inventory updates and the unique order record share one transaction. A duplicate order returns
`409`; unresolved, inactive, or mismatched rows return `422`. Either failure leaves inventory
unchanged.
