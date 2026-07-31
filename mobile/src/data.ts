import { Platform } from 'react-native';
import { File } from 'expo-file-system';
import { localStore } from './localStore';
import {
  Dashboard,
  HistoryEntry,
  Meal,
  MealExtraction,
  MealInput,
  MealTemplate,
  PackingSlipManifest,
  PickResult,
  ShipmentConfirmation,
  ShipmentConfirmationResult
} from './types';

const apiBase = process.env.EXPO_PUBLIC_API_URL?.replace(/\/$/, '');

/** Active persistence adapter selected from Expo's public API URL setting. */
export const dataMode = apiBase ? 'server' : 'local';

/**
 * Sends a JSON API request and converts non-success problem responses to errors.
 *
 * @param path backend-relative API path
 * @param options optional Fetch request options
 */
async function request<T>(path: string, options?: RequestInit): Promise<T> {
  if (!apiBase) throw new Error('Server mode is not configured');
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) }
  });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json() as { detail?: string; message?: string };
      message = body.detail || body.message || message;
    } catch { /* ignore non-JSON error */ }
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

/**
 * Appends an image to multipart data using the platform-appropriate file API.
 */
async function appendPhoto(
  form: FormData,
  field: string,
  uri: string,
  mimeType: string,
  fileName: string
) {
  if (Platform.OS === 'web') {
    const blob = await (await fetch(uri)).blob();
    form.append(field, blob.slice(0, blob.size, mimeType), fileName);
    return;
  }
  const file = new File(uri);
  form.append(field, file, fileName);
}

/**
 * Unified MealDeck data API. Each inventory method delegates to the backend
 * when configured and otherwise mirrors the same semantics in local storage.
 */
export const data = {
  listMeals: (): Promise<Meal[]> => apiBase ? request('/api/meals') : localStore.listMeals(),
  addMeal: (input: MealInput): Promise<Meal> => apiBase
    ? request('/api/meals', { method: 'POST', body: JSON.stringify(input) })
    : localStore.addMeal(input),
  updateMeal: (id: string, input: MealInput): Promise<Meal> => apiBase
    ? request(`/api/meals/${id}`, { method: 'PUT', body: JSON.stringify(input) })
    : localStore.updateMeal(id, input),
  deleteMeal: (id: string): Promise<void> => apiBase
    ? request(`/api/meals/${id}`, { method: 'DELETE' })
    : localStore.deleteMeal(id),
  lookupTemplate: (identifier: string): Promise<MealTemplate> => apiBase
    ? request(`/api/meal-templates/lookup?identifier=${encodeURIComponent(identifier)}`)
    : localStore.lookupTemplate(identifier),
  confirmShipment: (input: ShipmentConfirmation): Promise<ShipmentConfirmationResult> => apiBase
    ? request('/api/shipments/confirm', { method: 'POST', body: JSON.stringify(input) })
    : localStore.confirmShipment(input),
  previewRandom: (avoidDays = 7, allowRecent = false, excludeMealId?: string): Promise<Meal> => {
    const exclude = excludeMealId ? `&excludeMealId=${encodeURIComponent(excludeMealId)}` : '';
    return apiBase
      ? request(`/api/picks/preview?avoidDays=${avoidDays}&allowRecent=${allowRecent}${exclude}`, { method: 'POST' })
      : localStore.previewRandom(avoidDays, allowRecent, excludeMealId);
  },
  consume: (id: string, avoidDays = 7): Promise<PickResult> => apiBase
    ? request(`/api/meals/${id}/consume?avoidDays=${avoidDays}`, { method: 'POST' })
    : localStore.consume(id, avoidDays),
  listHistory: (): Promise<HistoryEntry[]> => apiBase ? request('/api/history') : localStore.listHistory(),
  undo: (id: string): Promise<Meal> => apiBase
    ? request(`/api/history/${id}/undo`, { method: 'POST' })
    : localStore.undo(id),
  dashboard: (avoidDays = 7): Promise<Dashboard> => apiBase
    ? request(`/api/dashboard?avoidDays=${avoidDays}`)
    : localStore.dashboard(avoidDays),
  async extractMeal(frontUri: string, backUri: string, frontMime = 'image/jpeg', backMime = 'image/jpeg'): Promise<MealExtraction> {
    if (!apiBase) throw new Error('Automatic photo reading requires the MealDeck backend.');
    const form = new FormData();
    await appendPhoto(form, 'front', frontUri, frontMime, 'front-meal-card.jpg');
    await appendPhoto(form, 'back', backUri, backMime, 'back-cooking-guide.jpg');
    const response = await fetch(`${apiBase}/api/extractions`, { method: 'POST', body: form });
    if (!response.ok) {
      let message = 'The meal photos could not be read.';
      try {
        const body = await response.json() as { detail?: string; message?: string };
        message = body.detail || body.message || message;
      } catch { /* ignore non-JSON error */ }
      throw new Error(message);
    }
    return response.json() as Promise<MealExtraction>;
  },
  async extractPackingSlip(
    uri: string,
    mimeType = 'image/jpeg',
    fileName = 'packing-slip.jpg'
  ): Promise<PackingSlipManifest> {
    if (!apiBase) throw new Error('Packing-slip reading requires the MealDeck backend.');
    const form = new FormData();
    await appendPhoto(form, 'slip', uri, mimeType, fileName);
    const response = await fetch(`${apiBase}/api/packing-slips/extract`, {
      method: 'POST',
      body: form
    });
    if (!response.ok) {
      let message = 'The packing slip could not be read.';
      try {
        const body = await response.json() as { detail?: string; message?: string };
        message = body.detail || body.message || message;
      } catch { /* ignore non-JSON error */ }
      throw new Error(message);
    }
    return response.json() as Promise<PackingSlipManifest>;
  },
  async uploadImage(uri: string, mimeType = 'image/jpeg', fileName = 'meal.jpg'): Promise<string> {
    if (!apiBase) return uri;
    const form = new FormData();
    await appendPhoto(form, 'file', uri, mimeType, fileName);
    const response = await fetch(`${apiBase}/api/uploads`, { method: 'POST', body: form });
    if (!response.ok) throw new Error('Could not upload the meal photo');
    const body = await response.json() as { imageUrl: string };
    return `${apiBase}${body.imageUrl}`;
  }
};
