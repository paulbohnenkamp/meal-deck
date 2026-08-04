/** A consolidated inventory meal whose nutrition values are per serving. */
export type Meal = {
  id: string;
  name: string;
  sides?: string | null;
  category?: string | null;
  cookingMealCode?: string | null;
  frontBarcodePayload?: string | null;
  backQrPayload?: string | null;
  quantity: number;
  servings: number;
  caloriesPerServing?: number | null;
  carbsPerServing?: number | null;
  proteinPerServing?: number | null;
  fatPerServing?: number | null;
  sodiumMgPerServing?: number | null;
  imageUrl?: string | null;
  cookingGuideImageUrl?: string | null;
  source?: string | null;
  createdAt: string;
  updatedAt: string;
};

/** Editable values accepted when a meal is created or updated. */
export type MealInput = {
  name: string;
  sides?: string;
  category?: string;
  cookingMealCode?: string | null;
  frontBarcodePayload?: string | null;
  backQrPayload?: string | null;
  quantity: number;
  caloriesPerServing?: number | null;
  carbsPerServing?: number | null;
  proteinPerServing?: number | null;
  fatPerServing?: number | null;
  sodiumMgPerServing?: number | null;
  imageUrl?: string | null;
  cookingGuideImageUrl?: string | null;
  source?: string;
};

/** Reviewable fields extracted from meal-card photos by the backend. */
export type MealExtraction = {
  name?: string | null;
  sides?: string | null;
  category?: string | null;
  frontCookingMealCode?: string | null;
  backCookingMealCode?: string | null;
  cookingMealCode?: string | null;
  frontBarcodePayload?: string | null;
  backQrPayload?: string | null;
  caloriesPerServing?: number | null;
  carbsPerServing?: number | null;
  proteinPerServing?: number | null;
  fatPerServing?: number | null;
  sodiumMgPerServing?: number | null;
};

/** Active revision of a reusable, previously reviewed meal definition. */
export type MealTemplate = {
  id: string;
  provider: string;
  revision: number;
  active?: boolean;
  cookingMealCode?: string | null;
  frontBarcodePayload?: string | null;
  backQrPayload?: string | null;
  name: string;
  sides?: string | null;
  category?: string | null;
  caloriesPerServing?: number | null;
  carbsPerServing?: number | null;
  proteinPerServing?: number | null;
  fatPerServing?: number | null;
  sodiumMgPerServing?: number | null;
  imageUrl?: string | null;
  cookingGuideImageUrl?: string | null;
  verifiedAt: string;
};

/** One extracted shipment row with conservative template-match status. */
export type PackingSlipLine = {
  itemCode?: string | null;
  quantity: number;
  description?: string | null;
  status: 'KNOWN' | 'CHANGED' | 'UNKNOWN';
  template?: MealTemplate | null;
};

/** Review-only shipment manifest extracted from one packing-slip photo. */
export type PackingSlipManifest = {
  orderId?: string | null;
  shippedAt?: string | null;
  lines: PackingSlipLine[];
};

/** Fully resolved shipment submitted for atomic inventory confirmation. */
export type ShipmentConfirmation = {
  orderId: string;
  shippedAt?: string | null;
  lines: Array<{ templateId: string; itemCode?: string | null; quantity: number }>;
};

/** Result of one successful all-or-nothing shipment confirmation. */
export type ShipmentConfirmationResult = {
  orderId: string;
  totalBoxes: number;
  meals: Meal[];
  confirmedAt: string;
};

/** Immutable snapshot of one consumed two-serving meal box. */
export type HistoryEntry = {
  id: string;
  mealId?: string | null;
  mealName: string;
  carbsPerServing?: number | null;
  servings: number;
  consumedAt: string;
};

/** Atomic result of decrementing inventory and recording history. */
export type PickResult = {
  meal: Meal;
  history: HistoryEntry;
  avoidDays: number;
};

/** Inventory and strict-random-draw counts for the selected avoidance window. */
export type Dashboard = {
  mealTypes: number;
  totalBoxes: number;
  eligibleMealTypes: number;
  avoidDays: number;
};
