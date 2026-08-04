import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  Dashboard,
  HistoryEntry,
  Meal,
  MealInput,
  MealTemplate,
  PickResult,
  ShipmentConfirmation,
  ShipmentConfirmationResult
} from './types';

const MEALS_KEY = 'mealdeck.meals.v1';
const HISTORY_KEY = 'mealdeck.history.v1';
const STATE_KEY = 'mealdeck.state.v2';

/** Atomically persisted offline inventory and history document. */
type LocalState = {
  meals: Meal[];
  history: HistoryEntry[];
  templates: MealTemplate[];
  shipments: Array<{ orderId: string; confirmedAt: string }>;
};

const now = () => new Date().toISOString();
const id = () => `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
const normalize = (value: string) => value.toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim().replace(/\s+/g, ' ');
const normalizeSides = (value?: string | null) => value?.replace(/^\s*with\s+/i, '').trim() || undefined;

/** Migrates the former description field without discarding existing local data. */
function migrateMeal<T extends Meal | MealTemplate>(value: T & { description?: string | null }): T {
  const { description: legacySides, ...current } = value;
  return { ...current, sides: normalizeSides(value.sides ?? legacySides) } as T;
}

/** Converts an identifier-bearing legacy inventory row into a reusable template. */
function templateFromMeal(meal: Meal): MealTemplate | null {
  if (!meal.cookingMealCode && !meal.frontBarcodePayload && !meal.backQrPayload) return null;
  return {
    id: id(),
    provider: 'SUVIE',
    revision: 1,
    active: true,
    cookingMealCode: meal.cookingMealCode,
    frontBarcodePayload: meal.frontBarcodePayload,
    backQrPayload: meal.backQrPayload,
    name: meal.name,
    sides: meal.sides,
    category: meal.category,
    caloriesPerServing: meal.caloriesPerServing,
    carbsPerServing: meal.carbsPerServing,
    proteinPerServing: meal.proteinPerServing,
    fatPerServing: meal.fatPerServing,
    sodiumMgPerServing: meal.sodiumMgPerServing,
    imageUrl: meal.imageUrl,
    cookingGuideImageUrl: meal.cookingGuideImageUrl,
    verifiedAt: meal.updatedAt
  };
}

let operationQueue = Promise.resolve();

/**
 * Serializes local mutations so inventory and history remain one logical
 * transaction even when UI actions overlap.
 */
function serialized<T>(operation: () => Promise<T>): Promise<T> {
  const result = operationQueue.then(operation, operation);
  operationQueue = result.then(() => undefined, () => undefined);
  return result;
}

/** Reads current state and migrates the original split-key storage format. */
async function readState(): Promise<LocalState> {
  const raw = await AsyncStorage.getItem(STATE_KEY);
  if (raw) {
    const stored = JSON.parse(raw) as Omit<LocalState, 'templates' | 'shipments'> & {
      templates?: MealTemplate[];
      shipments?: Array<{ orderId: string; confirmedAt: string }>;
    };
    return {
      ...stored,
      meals: stored.meals.map(meal => migrateMeal(meal)),
      templates: stored.templates?.map(template => migrateMeal(template))
        ?? stored.meals.map(meal => templateFromMeal(migrateMeal(meal)))
          .filter((value): value is MealTemplate => value != null),
      shipments: stored.shipments ?? []
    };
  }

  const legacy = await AsyncStorage.multiGet([MEALS_KEY, HISTORY_KEY]);
  const legacyMeals = legacy[0][1];
  const legacyHistory = legacy[1][1];
  const state: LocalState = {
    meals: legacyMeals
      ? (JSON.parse(legacyMeals) as Array<Meal & { description?: string | null }>).map(migrateMeal)
      : [],
    history: legacyHistory ? JSON.parse(legacyHistory) as HistoryEntry[] : [],
    templates: [],
    shipments: []
  };
  await writeState(state);
  return state;
}

/** Persists inventory and history together as one state document. */
async function writeState(state: LocalState) {
  await AsyncStorage.setItem(STATE_KEY, JSON.stringify(state));
}

/** Applies the atomic inventory decrement and history insertion in memory. */
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

/** Stores a new local template revision when reviewed meal fields changed. */
function rememberTemplate(state: LocalState, input: MealInput) {
  const identifiers = [
    input.cookingMealCode,
    input.frontBarcodePayload,
    input.backQrPayload
  ].map(value => value?.trim()).filter((value): value is string => Boolean(value));
  if (!identifiers.length) return;
  const current = state.templates.find(template => template.active !== false && identifiers.some(
    value => value === template.cookingMealCode
      || value === template.frontBarcodePayload
      || value === template.backQrPayload
  ));
  const definition = {
    cookingMealCode: input.cookingMealCode,
    frontBarcodePayload: input.frontBarcodePayload,
    backQrPayload: input.backQrPayload,
    name: input.name.trim(),
    sides: input.sides,
    category: input.category,
    caloriesPerServing: input.caloriesPerServing,
    carbsPerServing: input.carbsPerServing,
    proteinPerServing: input.proteinPerServing,
    fatPerServing: input.fatPerServing,
    sodiumMgPerServing: input.sodiumMgPerServing,
    imageUrl: input.imageUrl,
    cookingGuideImageUrl: input.cookingGuideImageUrl
  };
  if (current && templateDefinition(current) === JSON.stringify(definition)) return;
  if (current) current.active = false;
  state.templates.push({
    ...definition,
    id: id(),
    provider: 'SUVIE',
    revision: current ? current.revision + 1 : 1,
    active: true,
    verifiedAt: now()
  });
}

function templateDefinition(template: MealTemplate) {
  return JSON.stringify({
    cookingMealCode: template.cookingMealCode,
    frontBarcodePayload: template.frontBarcodePayload,
    backQrPayload: template.backQrPayload,
    name: template.name,
    sides: template.sides,
    category: template.category,
    caloriesPerServing: template.caloriesPerServing,
    carbsPerServing: template.carbsPerServing,
    proteinPerServing: template.proteinPerServing,
    fatPerServing: template.fatPerServing,
    sodiumMgPerServing: template.sodiumMgPerServing,
    imageUrl: template.imageUrl,
    cookingGuideImageUrl: template.cookingGuideImageUrl
  });
}

/**
 * Offline implementation of the MealDeck data contract, including normalized
 * duplicate consolidation, strict random eligibility, and one-box undo.
 */
export const localStore = {
  async listMeals() {
    return serialized(async () => (await readState()).meals.sort((a, b) => a.name.localeCompare(b.name)));
  },

  async addMeal(input: MealInput): Promise<Meal> {
    return serialized(async () => {
      const state = await readState();
      const cookingCode = input.cookingMealCode?.trim();
      const existing = state.meals.find(meal =>
        Boolean(cookingCode) && meal.cookingMealCode === cookingCode)
        ?? state.meals.find(meal => normalize(meal.name) === normalize(input.name));
      if (existing) {
        Object.assign(existing, input, {
          id: existing.id,
          quantity: existing.quantity + input.quantity,
          servings: 2,
          createdAt: existing.createdAt,
          updatedAt: now()
        });
        rememberTemplate(state, input);
        await writeState(state);
        return existing;
      }
      const meal: Meal = { ...input, id: id(), servings: 2, createdAt: now(), updatedAt: now() };
      state.meals.push(meal);
      rememberTemplate(state, input);
      await writeState(state);
      return meal;
    });
  },

  async updateMeal(mealId: string, input: MealInput): Promise<Meal> {
    return serialized(async () => {
      const state = await readState();
      const index = state.meals.findIndex(meal => meal.id === mealId);
      if (index < 0) throw new Error('Meal not found');
      const confirmedCode = state.meals[index].cookingMealCode?.trim() || null;
      const requestedCode = input.cookingMealCode?.trim() || null;
      if (confirmedCode !== requestedCode) {
        throw new Error('A confirmed cooking meal code cannot be changed.');
      }
      const duplicate = state.meals.some(meal =>
        meal.id !== mealId && normalize(meal.name) === normalize(input.name));
      if (duplicate) throw new Error('Another meal already has that name.');
      state.meals[index] = {
        ...state.meals[index],
        ...input,
        id: mealId,
        cookingMealCode: state.meals[index].cookingMealCode,
        servings: 2,
        updatedAt: now()
      };
      rememberTemplate(state, input);
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

  async lookupTemplate(identifier: string): Promise<MealTemplate> {
    return serialized(async () => {
      const value = identifier.trim();
      const template = (await readState()).templates.find(item =>
        item.active !== false && (
          item.cookingMealCode === value
          || item.frontBarcodePayload === value
          || item.backQrPayload === value
        ));
      if (!template) throw new Error('No reviewed meal template matches that identifier.');
      return template;
    });
  },

  async confirmShipment(request: ShipmentConfirmation): Promise<ShipmentConfirmationResult> {
    return serialized(async () => {
      const state = await readState();
      const orderId = request.orderId.trim();
      if (!orderId) throw new Error('Order ID is required.');
      if (state.shipments.some(shipment => shipment.orderId === orderId)) {
        throw new Error('That shipment has already been confirmed.');
      }
      const resolved = request.lines.map(line => {
        if (!Number.isInteger(line.quantity) || line.quantity < 1) {
          throw new Error('Every shipment quantity must be at least one box.');
        }
        const template = state.templates.find(item =>
          item.id === line.templateId && item.active !== false);
        if (!template) throw new Error('Every shipment row must reference an active reviewed template.');
        if (line.itemCode && ![
          template.cookingMealCode,
          template.frontBarcodePayload,
          template.backQrPayload
        ].includes(line.itemCode)) {
          throw new Error('A shipment item code no longer matches its reviewed template.');
        }
        return { template, quantity: line.quantity };
      });
      if (!resolved.length) throw new Error('Shipment lines are required.');

      const quantities = new Map<string, { template: MealTemplate; quantity: number }>();
      for (const line of resolved) {
        const key = normalize(line.template.name);
        const current = quantities.get(key);
        quantities.set(key, {
          template: line.template,
          quantity: line.quantity + (current?.quantity ?? 0)
        });
      }
      const updated: Meal[] = [];
      for (const { template, quantity } of quantities.values()) {
        const existing = state.meals.find(meal => normalize(meal.name) === normalize(template.name));
        const timestamp = now();
        const values: Meal = {
          ...template,
          id: existing?.id ?? id(),
          quantity: (existing?.quantity ?? 0) + quantity,
          servings: 2,
          source: 'SHIPMENT',
          createdAt: existing?.createdAt ?? timestamp,
          updatedAt: timestamp
        };
        if (existing) Object.assign(existing, values);
        else state.meals.push(values);
        updated.push(values);
      }
      const confirmedAt = now();
      state.shipments.push({ orderId, confirmedAt });
      await writeState(state);
      return {
        orderId,
        totalBoxes: resolved.reduce((sum, line) => sum + line.quantity, 0),
        meals: updated,
        confirmedAt
      };
    });
  },

  async previewRandom(avoidDays = 7, allowRecent = false, excludeMealId?: string): Promise<Meal> {
    return serialized(async () => {
      const state = await readState();
      const cutoff = Date.now() - avoidDays * 24 * 60 * 60 * 1000;
      const recent = new Set(state.history.filter(item => new Date(item.consumedAt).getTime() > cutoff).map(item => normalize(item.mealName)));
      const available = state.meals.filter(meal => meal.quantity > 0);
      if (!available.length) throw new Error('Your freezer inventory is empty.');
      const eligible = available.filter(meal => allowRecent || !recent.has(normalize(meal.name)));
      if (!eligible.length) throw new Error(`Every available meal was eaten in the last ${avoidDays} days. Try the relaxed draw.`);
      const choices = eligible.length > 1 && excludeMealId
        ? eligible.filter(meal => meal.id !== excludeMealId)
        : eligible;
      return choices[Math.floor(Math.random() * choices.length)];
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
        mealTypes: state.meals.filter(meal => meal.quantity > 0).length,
        totalBoxes: state.meals.reduce((sum, meal) => sum + meal.quantity, 0),
        eligibleMealTypes: state.meals.filter(meal => meal.quantity > 0 && !recent.has(normalize(meal.name))).length,
        avoidDays
      };
    });
  }
};
