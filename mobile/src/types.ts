export type Meal = {
  id: string;
  name: string;
  description?: string | null;
  category?: string | null;
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

export type MealInput = {
  name: string;
  description?: string;
  category?: string;
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

export type MealExtraction = {
  name?: string | null;
  description?: string | null;
  category?: string | null;
  caloriesPerServing?: number | null;
  carbsPerServing?: number | null;
  proteinPerServing?: number | null;
  fatPerServing?: number | null;
  sodiumMgPerServing?: number | null;
};

export type HistoryEntry = {
  id: string;
  mealId?: string | null;
  mealName: string;
  carbsPerServing?: number | null;
  servings: number;
  consumedAt: string;
};

export type PickResult = {
  meal: Meal;
  history: HistoryEntry;
  avoidDays: number;
};

export type Dashboard = {
  mealTypes: number;
  totalBoxes: number;
  eligibleMealTypes: number;
  avoidDays: number;
};
