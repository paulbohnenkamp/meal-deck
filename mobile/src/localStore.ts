import AsyncStorage from '@react-native-async-storage/async-storage';
import { Dashboard, HistoryEntry, Meal, MealInput, PickResult } from './types';

const MEALS_KEY = 'mealdeck.meals.v1';
const HISTORY_KEY = 'mealdeck.history.v1';
const STATE_KEY = 'mealdeck.state.v2';

type LocalState = { meals: Meal[]; history: HistoryEntry[] };

const now = () => new Date().toISOString();
const id = () => `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
const normalize = (value: string) => value.toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim().replace(/\s+/g, ' ');

const seed: Meal[] = [
  {
    id: 'sample-chicken-alfredo', name: 'Chicken Alfredo', description: 'Pasta with roasted chicken', category: 'Pasta',
    quantity: 2, servings: 2, caloriesPerServing: 610, carbsPerServing: 54, proteinPerServing: 35,
    fatPerServing: 28, sodiumMgPerServing: 940, source: 'SAMPLE', createdAt: now(), updatedAt: now()
  },
  {
    id: 'sample-beef-bulgogi', name: 'Beef Bulgogi', description: 'Korean-style beef with rice', category: 'Beef',
    quantity: 1, servings: 2, caloriesPerServing: 540, carbsPerServing: 62, proteinPerServing: 29,
    fatPerServing: 19, sodiumMgPerServing: 1080, source: 'SAMPLE', createdAt: now(), updatedAt: now()
  },
  {
    id: 'sample-tuscan-chicken', name: 'Tuscan Chicken', description: 'Chicken, vegetables and creamy sauce', category: 'Chicken',
    quantity: 2, servings: 2, caloriesPerServing: 430, carbsPerServing: 21, proteinPerServing: 38,
    fatPerServing: 22, sodiumMgPerServing: 790, source: 'SAMPLE', createdAt: now(), updatedAt: now()
  }
];

let operationQueue = Promise.resolve();

function serialized<T>(operation: () => Promise<T>): Promise<T> {
  const result = operationQueue.then(operation, operation);
  operationQueue = result.then(() => undefined, () => undefined);
  return result;
}

async function readState(): Promise<LocalState> {
  const raw = await AsyncStorage.getItem(STATE_KEY);
  if (raw) return JSON.parse(raw) as LocalState;

  const legacy = await AsyncStorage.multiGet([MEALS_KEY, HISTORY_KEY]);
  const legacyMeals = legacy[0][1];
  const legacyHistory = legacy[1][1];
  const state: LocalState = {
    meals: legacyMeals ? JSON.parse(legacyMeals) as Meal[] : seed.map(meal => ({ ...meal })),
    history: legacyHistory ? JSON.parse(legacyHistory) as HistoryEntry[] : []
  };
  await writeState(state);
  return state;
}

async function writeState(state: LocalState) {
  await AsyncStorage.setItem(STATE_KEY, JSON.stringify(state));
}

function consumeState(state: LocalState, mealId: string, avoidDays: number): PickResult {
  const meal = state.meals.find(item => item.id === mealId);
  if (!meal || meal.quantity <= 0) throw new Error('That meal is out of stock.');
  meal.quantity -= 1;
  meal.updatedAt = now();
  const entry: HistoryEntry = {
    id: id(), mealId: meal.id, mealName: meal.name, carbsPerServing: meal.carbsPerServing,
    servings: meal.servings, consumedAt: now()
  };
  state.history.unshift(entry);
  return { meal, history: entry, avoidDays };
}

export const localStore = {
  async listMeals() {
    return serialized(async () => (await readState()).meals.sort((a, b) => a.name.localeCompare(b.name)));
  },

  async addMeal(input: MealInput): Promise<Meal> {
    return serialized(async () => {
      const state = await readState();
      const existing = state.meals.find(meal => normalize(meal.name) === normalize(input.name));
      if (existing) {
        Object.assign(existing, input, {
          id: existing.id,
          quantity: existing.quantity + input.quantity,
          servings: 2,
          createdAt: existing.createdAt,
          updatedAt: now()
        });
        await writeState(state);
        return existing;
      }
      const meal: Meal = { ...input, id: id(), servings: 2, createdAt: now(), updatedAt: now() };
      state.meals.push(meal);
      await writeState(state);
      return meal;
    });
  },

  async updateMeal(mealId: string, input: MealInput): Promise<Meal> {
    return serialized(async () => {
      const state = await readState();
      const index = state.meals.findIndex(meal => meal.id === mealId);
      if (index < 0) throw new Error('Meal not found');
      state.meals[index] = { ...state.meals[index], ...input, id: mealId, servings: 2, updatedAt: now() };
      await writeState(state);
      return state.meals[index];
    });
  },

  async deleteMeal(mealId: string) {
    return serialized(async () => {
      const state = await readState();
      state.meals = state.meals.filter(meal => meal.id !== mealId);
      await writeState(state);
    });
  },

  async pickRandom(avoidDays = 7, allowRecent = false): Promise<PickResult> {
    return serialized(async () => {
      const state = await readState();
      const cutoff = Date.now() - avoidDays * 24 * 60 * 60 * 1000;
      const recent = new Set(state.history.filter(item => new Date(item.consumedAt).getTime() > cutoff).map(item => normalize(item.mealName)));
      const available = state.meals.filter(meal => meal.quantity > 0);
      if (!available.length) throw new Error('Your freezer inventory is empty.');
      const eligible = available.filter(meal => allowRecent || !recent.has(normalize(meal.name)));
      if (!eligible.length) throw new Error(`Every available meal was eaten in the last ${avoidDays} days. Try the relaxed draw.`);
      const selected = eligible[Math.floor(Math.random() * eligible.length)];
      const result = consumeState(state, selected.id, avoidDays);
      await writeState(state);
      return result;
    });
  },

  async consume(mealId: string, avoidDays = 7): Promise<PickResult> {
    return serialized(async () => {
      const state = await readState();
      const result = consumeState(state, mealId, avoidDays);
      await writeState(state);
      return result;
    });
  },

  async listHistory() {
    return serialized(async () => (await readState()).history.sort((a, b) => b.consumedAt.localeCompare(a.consumedAt)));
  },

  async undo(historyId: string): Promise<Meal> {
    return serialized(async () => {
      const state = await readState();
      const entry = state.history.find(item => item.id === historyId);
      if (!entry) throw new Error('History entry not found');
      let meal = state.meals.find(item => item.id === entry.mealId);
      if (!meal) {
        meal = {
          id: id(), name: entry.mealName, quantity: 0, servings: entry.servings,
          carbsPerServing: entry.carbsPerServing, source: 'RESTORED', createdAt: now(), updatedAt: now()
        };
        state.meals.push(meal);
      }
      meal.quantity += 1;
      meal.updatedAt = now();
      state.history = state.history.filter(item => item.id !== historyId);
      await writeState(state);
      return meal;
    });
  },

  async dashboard(avoidDays = 7): Promise<Dashboard> {
    return serialized(async () => {
      const state = await readState();
      const cutoff = Date.now() - avoidDays * 24 * 60 * 60 * 1000;
      const recent = new Set(state.history.filter(item => new Date(item.consumedAt).getTime() > cutoff).map(item => normalize(item.mealName)));
      return {
        mealTypes: state.meals.length,
        totalBoxes: state.meals.reduce((sum, meal) => sum + meal.quantity, 0),
        eligibleMealTypes: state.meals.filter(meal => meal.quantity > 0 && !recent.has(normalize(meal.name))).length,
        avoidDays
      };
    });
  }
};
