import { Platform } from 'react-native';
import { localStore } from './localStore';
import { Dashboard, HistoryEntry, Meal, MealInput, PickResult } from './types';

const apiBase = process.env.EXPO_PUBLIC_API_URL?.replace(/\/$/, '');
export const dataMode = apiBase ? 'server' : 'local';

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
  pickRandom: (avoidDays = 7, allowRecent = false): Promise<PickResult> => apiBase
    ? request(`/api/picks/random?avoidDays=${avoidDays}&allowRecent=${allowRecent}`, { method: 'POST' })
    : localStore.pickRandom(avoidDays, allowRecent),
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
  async uploadImage(uri: string, mimeType = 'image/jpeg', fileName = 'meal.jpg'): Promise<string> {
    if (!apiBase) return uri;
    const form = new FormData();
    if (Platform.OS === 'web') {
      const blob = await (await fetch(uri)).blob();
      form.append('file', blob, fileName);
    } else {
      form.append('file', { uri, type: mimeType, name: fileName } as unknown as Blob);
    }
    const response = await fetch(`${apiBase}/api/uploads`, { method: 'POST', body: form });
    if (!response.ok) throw new Error('Could not upload the meal photo');
    const body = await response.json() as { imageUrl: string };
    return `${apiBase}${body.imageUrl}`;
  }
};
