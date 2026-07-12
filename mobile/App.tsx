import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  RefreshControl,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { StatusBar } from 'expo-status-bar';
import { Card, MealImage, NutritionRow, Pill, PrimaryButton } from './src/components';
import { data, dataMode } from './src/data';
import { colors } from './src/theme';
import { Dashboard, HistoryEntry, Meal, MealInput, PickResult } from './src/types';

type Tab = 'home' | 'inventory' | 'add' | 'history';

type FormState = {
  name: string;
  description: string;
  category: string;
  quantity: string;
  calories: string;
  carbs: string;
  protein: string;
  fat: string;
  sodium: string;
  imageUri: string;
  imageBase64?: string;
  imageMime?: string;
  imageName?: string;
};

const emptyForm: FormState = {
  name: '', description: '', category: '', quantity: '1', calories: '', carbs: '',
  protein: '', fat: '', sodium: '', imageUri: ''
};

export default function App() {
  const [tab, setTab] = useState<Tab>('home');
  const [meals, setMeals] = useState<Meal[]>([]);
  const [history, setHistory] = useState<HistoryEntry[]>([]);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [picked, setPicked] = useState<PickResult | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [search, setSearch] = useState('');

  const refresh = useCallback(async (manual = false) => {
    manual ? setRefreshing(true) : setLoading(true);
    try {
      const [mealList, historyList, stats] = await Promise.all([
        data.listMeals(), data.listHistory(), data.dashboard(7)
      ]);
      setMeals(mealList);
      setHistory(historyList);
      setDashboard(stats);
    } catch (error) {
      showError(error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { void refresh(); }, [refresh]);

  const filteredMeals = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return meals;
    return meals.filter(meal => [meal.name, meal.description, meal.category].some(value => value?.toLowerCase().includes(query)));
  }, [meals, search]);

  async function drawDinner(allowRecent = false) {
    setBusy(true);
    setNotice(null);
    try {
      const result = await data.pickRandom(7, allowRecent);
      setPicked(result);
      await refresh();
    } catch (error) {
      const message = errorMessage(error);
      if (message.toLowerCase().includes('relaxed draw')) {
        setNotice(message);
      } else {
        showError(error);
      }
    } finally {
      setBusy(false);
    }
  }

  async function consumeMeal(meal: Meal) {
    setBusy(true);
    try {
      const result = await data.consume(meal.id, 7);
      setPicked(result);
      await refresh();
    } catch (error) {
      showError(error);
    } finally {
      setBusy(false);
    }
  }

  async function adjustQuantity(meal: Meal, delta: number) {
    const next = Math.max(0, meal.quantity + delta);
    setBusy(true);
    try {
      await data.updateMeal(meal.id, toInput(meal, next));
      await refresh();
    } catch (error) {
      showError(error);
    } finally {
      setBusy(false);
    }
  }

  async function removeMeal(meal: Meal) {
    if (!(await confirmAction(`Delete ${meal.name}?`, 'This removes every box of this meal from inventory.'))) return;
    setBusy(true);
    try {
      await data.deleteMeal(meal.id);
      await refresh();
    } catch (error) {
      showError(error);
    } finally {
      setBusy(false);
    }
  }

  async function undoHistory(entry: HistoryEntry) {
    setBusy(true);
    try {
      await data.undo(entry.id);
      await refresh();
      setNotice(`${entry.mealName} was returned to the freezer.`);
    } catch (error) {
      showError(error);
    } finally {
      setBusy(false);
    }
  }

  async function choosePhoto(camera: boolean) {
    try {
      if (camera) {
        const permission = await ImagePicker.requestCameraPermissionsAsync();
        if (!permission.granted) {
          Alert.alert('Camera permission needed', 'Allow camera access to photograph the meal card.');
          return;
        }
      }
      const result = camera
        ? await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 0.5, base64: dataMode === 'local' })
        : await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.5, base64: dataMode === 'local' });
      if (result.canceled || !result.assets[0]) return;
      const asset = result.assets[0];
      setForm(current => ({
        ...current,
        imageUri: asset.uri,
        imageBase64: asset.base64 ?? undefined,
        imageMime: asset.mimeType ?? 'image/jpeg',
        imageName: asset.fileName ?? 'meal.jpg'
      }));
    } catch (error) {
      showError(error);
    }
  }

  async function saveMeal() {
    if (!form.name.trim()) {
      Alert.alert('Meal name required', 'Enter the name shown on the prepared meal card.');
      return;
    }
    setBusy(true);
    try {
      let imageUrl: string | null = null;
      if (form.imageUri) {
        if (dataMode === 'local' && form.imageBase64) {
          imageUrl = `data:${form.imageMime ?? 'image/jpeg'};base64,${form.imageBase64}`;
        } else {
          imageUrl = await data.uploadImage(form.imageUri, form.imageMime, form.imageName);
        }
      }
      const input: MealInput = {
        name: form.name.trim(),
        description: clean(form.description),
        category: clean(form.category),
        quantity: Math.max(1, parseInt(form.quantity, 10) || 1),
        caloriesPerServing: numberOrNull(form.calories),
        carbsPerServing: numberOrNull(form.carbs),
        proteinPerServing: numberOrNull(form.protein),
        fatPerServing: numberOrNull(form.fat),
        sodiumMgPerServing: numberOrNull(form.sodium),
        imageUrl,
        source: form.imageUri ? 'PHOTO' : 'MANUAL'
      };
      const saved = await data.addMeal(input);
      setForm(emptyForm);
      await refresh();
      setNotice(`${saved.name} is in the freezer${saved.quantity > input.quantity ? ` (${saved.quantity} boxes total)` : ''}.`);
      setTab('inventory');
    } catch (error) {
      showError(error);
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <SafeAreaView style={styles.loadingScreen}>
        <StatusBar style="dark" />
        <View style={styles.logoMark}><Text style={styles.logoEmoji}>M</Text></View>
        <Text style={styles.loadingTitle}>MealDeck</Text>
        <ActivityIndicator size="large" color={colors.accent} style={{ marginTop: 24 }} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar style="dark" />
      <View style={styles.shell}>
        <Header mode={dataMode} />
        {notice && (
          <Pressable onPress={() => setNotice(null)} style={styles.notice}>
            <Text style={styles.noticeText}>{notice}</Text><Text style={styles.noticeClose}>×</Text>
          </Pressable>
        )}
        <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.content}>
          {tab === 'home' && (
            <HomeScreen
              dashboard={dashboard}
              history={history}
              meals={meals}
              busy={busy}
              onDraw={() => void drawDinner(false)}
              onRelaxedDraw={() => void drawDinner(true)}
              showRelaxed={Boolean(notice?.toLowerCase().includes('relaxed draw'))}
              refreshing={refreshing}
              refresh={() => void refresh(true)}
            />
          )}
          {tab === 'inventory' && (
            <InventoryScreen
              meals={filteredMeals}
              search={search}
              setSearch={setSearch}
              busy={busy}
              onConsume={meal => void consumeMeal(meal)}
              onIncrement={meal => void adjustQuantity(meal, 1)}
              onDecrement={meal => void adjustQuantity(meal, -1)}
              onDelete={meal => void removeMeal(meal)}
              onAdd={() => setTab('add')}
              refreshing={refreshing}
              refresh={() => void refresh(true)}
            />
          )}
          {tab === 'add' && (
            <AddScreen
              form={form}
              setForm={setForm}
              busy={busy}
              onCamera={() => void choosePhoto(true)}
              onLibrary={() => void choosePhoto(false)}
              onSave={() => void saveMeal()}
            />
          )}
          {tab === 'history' && (
            <HistoryScreen history={history} busy={busy} undo={entry => void undoHistory(entry)} refreshing={refreshing} refresh={() => void refresh(true)} />
          )}
        </KeyboardAvoidingView>
        <BottomNav tab={tab} setTab={setTab} />
      </View>
      <PickModal result={picked} close={() => setPicked(null)} />
      {busy && <View pointerEvents="none" style={styles.busyOverlay}><ActivityIndicator size="large" color={colors.accent} /></View>}
    </SafeAreaView>
  );
}

function Header({ mode }: { mode: string }) {
  return (
    <View style={styles.header}>
      <View style={styles.logoMark}><Text style={styles.logoEmoji}>M</Text></View>
      <View style={{ flex: 1 }}>
        <Text style={styles.brandName}>MealDeck</Text>
        <Text style={styles.brandTagline}>Freezer inventory · dinner decided</Text>
      </View>
      <Pill tone={mode === 'server' ? 'mint' : 'neutral'}>{mode === 'server' ? 'Synced' : 'On device'}</Pill>
    </View>
  );
}

function HomeScreen({ dashboard, history, meals, busy, onDraw, onRelaxedDraw, showRelaxed, refreshing, refresh }: {
  dashboard: Dashboard | null; history: HistoryEntry[]; meals: Meal[]; busy: boolean;
  onDraw: () => void; onRelaxedDraw: () => void; showRelaxed: boolean; refreshing: boolean; refresh: () => void;
}) {
  const latest = history.slice(0, 3);
  return (
    <ScrollView refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />} contentContainerStyle={styles.scrollContent}>
      <View style={styles.hero}>
        <Pill tone="orange">TONIGHT'S DINNER</Pill>
        <Text style={styles.heroTitle}>Let the freezer choose.</Text>
        <Text style={styles.heroBody}>MealDeck skips anything you ate during the last seven days, then removes the selected box from inventory.</Text>
        <PrimaryButton label={busy ? 'Drawing…' : 'Draw a meal'} onPress={onDraw} disabled={busy || !dashboard?.totalBoxes} />
        {showRelaxed && <Pressable onPress={onRelaxedDraw} style={styles.relaxedButton}><Text style={styles.relaxedText}>Draw anyway without the 7-day rule</Text></Pressable>}
      </View>

      <View style={styles.statsRow}>
        <Stat value={dashboard?.totalBoxes ?? 0} label="boxes" />
        <Stat value={dashboard?.mealTypes ?? 0} label="meal types" />
        <Stat value={dashboard?.eligibleMealTypes ?? 0} label="eligible now" />
      </View>

      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>Recently eaten</Text>
        <Text style={styles.sectionMeta}>7-day memory</Text>
      </View>
      {latest.length ? latest.map(entry => (
        <Card key={entry.id} style={styles.historyCompact}>
          <View style={styles.historyDot} />
          <View style={{ flex: 1 }}>
            <Text style={styles.cardTitle}>{entry.mealName}</Text>
            <Text style={styles.cardMuted}>{formatDate(entry.consumedAt)}</Text>
          </View>
          {entry.carbsPerServing != null && <Pill tone="mint">{entry.carbsPerServing * entry.servings}g box carbs</Pill>}
        </Card>
      )) : (
        <Card><Text style={styles.emptyTitle}>No dinner history yet</Text><Text style={styles.cardMuted}>Your first random draw will appear here.</Text></Card>
      )}

      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>Freezer preview</Text>
        <Text style={styles.sectionMeta}>{meals.filter(meal => meal.quantity > 0).length} stocked types</Text>
      </View>
      <View style={styles.previewGrid}>
        {meals.filter(meal => meal.quantity > 0).slice(0, 4).map(meal => (
          <Card key={meal.id} style={styles.previewCard}>
            <MealImage meal={meal} />
            <Text numberOfLines={2} style={styles.previewName}>{meal.name}</Text>
            <Pill tone="brand">× {meal.quantity}</Pill>
          </Card>
        ))}
      </View>
    </ScrollView>
  );
}

function InventoryScreen({ meals, search, setSearch, busy, onConsume, onIncrement, onDecrement, onDelete, onAdd, refreshing, refresh }: {
  meals: Meal[]; search: string; setSearch: (value: string) => void; busy: boolean;
  onConsume: (meal: Meal) => void; onIncrement: (meal: Meal) => void; onDecrement: (meal: Meal) => void;
  onDelete: (meal: Meal) => void; onAdd: () => void; refreshing: boolean; refresh: () => void;
}) {
  return (
    <ScrollView refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />} contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
      <View style={styles.titleRow}>
        <View><Text style={styles.pageTitle}>Freezer</Text><Text style={styles.pageSubtitle}>Each item is a two-person prepared meal.</Text></View>
        <Pressable onPress={onAdd} style={styles.addCircle}><Text style={styles.addCircleText}>＋</Text></Pressable>
      </View>
      <TextInput value={search} onChangeText={setSearch} placeholder="Search meals" placeholderTextColor="#92969B" style={styles.searchInput} />
      {meals.length ? meals.map(meal => (
        <Card key={meal.id} style={styles.mealCard}>
          <View style={styles.mealTop}>
            <MealImage meal={meal} />
            <View style={styles.mealInfo}>
              <View style={styles.mealNameRow}>
                <Text style={styles.cardTitle}>{meal.name}</Text>
                <Pill tone={meal.quantity ? 'brand' : 'neutral'}>× {meal.quantity}</Pill>
              </View>
              {meal.description ? <Text numberOfLines={2} style={styles.cardMuted}>{meal.description}</Text> : null}
              <View style={styles.inlinePills}>
                {meal.category ? <Pill tone="neutral">{meal.category}</Pill> : null}
                <Pill tone="mint">2 servings</Pill>
              </View>
            </View>
          </View>
          <NutritionRow meal={meal} />
          <View style={styles.actionRow}>
            <View style={styles.stepper}>
              <Pressable disabled={busy || meal.quantity <= 0} onPress={() => onDecrement(meal)} style={styles.stepperButton}><Text style={styles.stepperText}>−</Text></Pressable>
              <Text style={styles.stepperValue}>{meal.quantity}</Text>
              <Pressable disabled={busy} onPress={() => onIncrement(meal)} style={styles.stepperButton}><Text style={styles.stepperText}>＋</Text></Pressable>
            </View>
            <Pressable disabled={busy || meal.quantity <= 0} onPress={() => onConsume(meal)} style={[styles.smallAction, meal.quantity <= 0 && styles.disabled]}>
              <Text style={styles.smallActionText}>Eat this</Text>
            </Pressable>
            <Pressable disabled={busy} onPress={() => onDelete(meal)} style={styles.deleteAction}><Text style={styles.deleteText}>Delete</Text></Pressable>
          </View>
        </Card>
      )) : (
        <Card><Text style={styles.emptyTitle}>No matching meals</Text><Text style={styles.cardMuted}>Add a box or clear the search.</Text></Card>
      )}
    </ScrollView>
  );
}

function AddScreen({ form, setForm, busy, onCamera, onLibrary, onSave }: {
  form: FormState; setForm: React.Dispatch<React.SetStateAction<FormState>>; busy: boolean;
  onCamera: () => void; onLibrary: () => void; onSave: () => void;
}) {
  const field = (key: keyof FormState, value: string) => setForm(current => ({ ...current, [key]: value }));
  return (
    <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
      <Text style={styles.pageTitle}>Add a meal</Text>
      <Text style={styles.pageSubtitle}>Photograph the box or meal sheet, then verify the nutrition label.</Text>

      <Card style={styles.photoCard}>
        {form.imageUri ? <Image source={{ uri: form.imageUri }} style={styles.photoPreview} resizeMode="cover" /> : (
          <View style={styles.photoEmpty}><Text style={styles.photoEmoji}>📷</Text><Text style={styles.emptyTitle}>Meal-card photo</Text><Text style={styles.cardMuted}>The image stays with the inventory item.</Text></View>
        )}
        <View style={styles.photoActions}>
          <Pressable onPress={onCamera} style={styles.photoButton}><Text style={styles.photoButtonText}>Take photo</Text></Pressable>
          <Pressable onPress={onLibrary} style={styles.photoButton}><Text style={styles.photoButtonText}>Choose photo</Text></Pressable>
        </View>
      </Card>

      <Card>
        <Field label="Meal name *" value={form.name} onChangeText={value => field('name', value)} placeholder="Chicken Tikka Masala" />
        <Field label="Description" value={form.description} onChangeText={value => field('description', value)} placeholder="Chicken with basmati rice" multiline />
        <View style={styles.twoColumns}>
          <Field compact label="Category" value={form.category} onChangeText={value => field('category', value)} placeholder="Chicken" />
          <Field compact label="Boxes" value={form.quantity} onChangeText={value => field('quantity', value)} placeholder="1" keyboardType="number-pad" />
        </View>
      </Card>

      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>Nutrition per serving</Text>
        <Pill tone="mint">Always 2 servings</Pill>
      </View>
      <Card>
        <View style={styles.twoColumns}>
          <Field compact label="Carbs (g)" value={form.carbs} onChangeText={value => field('carbs', value)} keyboardType="number-pad" placeholder="42" />
          <Field compact label="Calories" value={form.calories} onChangeText={value => field('calories', value)} keyboardType="number-pad" placeholder="510" />
        </View>
        <View style={styles.twoColumns}>
          <Field compact label="Protein (g)" value={form.protein} onChangeText={value => field('protein', value)} keyboardType="number-pad" placeholder="31" />
          <Field compact label="Fat (g)" value={form.fat} onChangeText={value => field('fat', value)} keyboardType="number-pad" placeholder="18" />
        </View>
        <Field label="Sodium (mg)" value={form.sodium} onChangeText={value => field('sodium', value)} keyboardType="number-pad" placeholder="850" />
        {numberOrNull(form.carbs) != null && (
          <View style={styles.carbCallout}>
            <Text style={styles.carbLabel}>Whole box</Text>
            <Text style={styles.carbTotal}>{numberOrNull(form.carbs)! * 2}g carbs</Text>
          </View>
        )}
      </Card>
      <PrimaryButton label={busy ? 'Saving…' : 'Add to freezer'} onPress={onSave} disabled={busy} />
      <Text style={styles.formFootnote}>MVP note: the photo is captured now; automatic label-reading is planned for the next milestone, so nutrition is verified manually.</Text>
    </ScrollView>
  );
}

function HistoryScreen({ history, busy, undo, refreshing, refresh }: {
  history: HistoryEntry[]; busy: boolean; undo: (entry: HistoryEntry) => void; refreshing: boolean; refresh: () => void;
}) {
  return (
    <ScrollView refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />} contentContainerStyle={styles.scrollContent}>
      <Text style={styles.pageTitle}>Dinner history</Text>
      <Text style={styles.pageSubtitle}>Used to keep recent meals out of the random draw.</Text>
      {history.length ? history.map(entry => (
        <Card key={entry.id} style={styles.historyCard}>
          <View style={styles.calendarBadge}>
            <Text style={styles.calendarMonth}>{new Date(entry.consumedAt).toLocaleDateString(undefined, { month: 'short' }).toUpperCase()}</Text>
            <Text style={styles.calendarDay}>{new Date(entry.consumedAt).getDate()}</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.cardTitle}>{entry.mealName}</Text>
            <Text style={styles.cardMuted}>{formatDate(entry.consumedAt)}</Text>
            {entry.carbsPerServing != null && <Text style={styles.historyNutrition}>{entry.carbsPerServing}g carbs per serving · {entry.carbsPerServing * entry.servings}g per box</Text>}
          </View>
          <Pressable disabled={busy} onPress={() => undo(entry)} style={styles.undoButton}><Text style={styles.undoText}>Undo</Text></Pressable>
        </Card>
      )) : (
        <Card><Text style={styles.emptyTitle}>Nothing eaten yet</Text><Text style={styles.cardMuted}>A selected or manually eaten meal will show here.</Text></Card>
      )}
    </ScrollView>
  );
}

function PickModal({ result, close }: { result: PickResult | null; close: () => void }) {
  if (!result) return null;
  const carbs = result.meal.carbsPerServing;
  return (
    <Modal visible transparent animationType="fade" onRequestClose={close}>
      <View style={styles.modalBackdrop}>
        <View style={styles.modalCard}>
          <Text style={styles.modalEyebrow}>DINNER IS DECIDED</Text>
          <MealImage meal={result.meal} large />
          <Text style={styles.modalTitle}>{result.meal.name}</Text>
          {result.meal.description ? <Text style={styles.modalBody}>{result.meal.description}</Text> : null}
          <View style={styles.modalPills}>
            <Pill tone="mint">2 servings</Pill>
            {carbs != null && <Pill tone="orange">{carbs * 2}g carbs per box</Pill>}
          </View>
          <NutritionRow meal={result.meal} />
          <View style={styles.inventoryMessage}><Text style={styles.inventoryMessageText}>✓ One box was removed. {result.meal.quantity} remaining.</Text></View>
          <PrimaryButton label="Great — let's eat" onPress={close} />
        </View>
      </View>
    </Modal>
  );
}

function BottomNav({ tab, setTab }: { tab: Tab; setTab: (tab: Tab) => void }) {
  const items: Array<[Tab, string, string]> = [
    ['home', '⌂', 'Home'], ['inventory', '▦', 'Freezer'], ['add', '＋', 'Add'], ['history', '◷', 'History']
  ];
  return (
    <View style={styles.bottomNav}>
      {items.map(([value, icon, label]) => (
        <Pressable key={value} onPress={() => setTab(value)} style={styles.navItem}>
          <View style={[styles.navIconWrap, tab === value && styles.navIconActive]}><Text style={[styles.navIcon, tab === value && styles.navIconTextActive]}>{icon}</Text></View>
          <Text style={[styles.navLabel, tab === value && styles.navLabelActive]}>{label}</Text>
        </Pressable>
      ))}
    </View>
  );
}

function Field({ label, compact, ...inputProps }: { label: string; compact?: boolean } & React.ComponentProps<typeof TextInput>) {
  return (
    <View style={[styles.field, compact && styles.fieldCompact]}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput {...inputProps} placeholderTextColor="#9A9DA0" style={[styles.input, inputProps.multiline && styles.inputMultiline]} />
    </View>
  );
}

function Stat({ value, label }: { value: number; label: string }) {
  return <Card style={styles.statCard}><Text style={styles.statValue}>{value}</Text><Text style={styles.statLabel}>{label}</Text></Card>;
}

function toInput(meal: Meal, quantity: number): MealInput {
  return {
    name: meal.name, description: meal.description ?? undefined, category: meal.category ?? undefined, quantity,
    caloriesPerServing: meal.caloriesPerServing, carbsPerServing: meal.carbsPerServing,
    proteinPerServing: meal.proteinPerServing, fatPerServing: meal.fatPerServing,
    sodiumMgPerServing: meal.sodiumMgPerServing, imageUrl: meal.imageUrl, source: meal.source ?? undefined
  };
}

function clean(value: string) { return value.trim() || undefined; }
function numberOrNull(value: string) { const parsed = Number(value); return value.trim() && Number.isFinite(parsed) ? Math.round(parsed) : null; }
function errorMessage(error: unknown) { return error instanceof Error ? error.message : 'Something went wrong'; }
function showError(error: unknown) { Alert.alert('MealDeck', errorMessage(error)); }
function formatDate(value: string) { return new Date(value).toLocaleString(undefined, { weekday: 'short', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }); }

async function confirmAction(title: string, message: string): Promise<boolean> {
  if (Platform.OS === 'web') return globalThis.confirm(`${title}\n\n${message}`);
  return new Promise(resolve => Alert.alert(title, message, [
    { text: 'Cancel', style: 'cancel', onPress: () => resolve(false) },
    { text: 'Delete', style: 'destructive', onPress: () => resolve(true) }
  ]));
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  shell: { flex: 1, width: '100%', maxWidth: 880, alignSelf: 'center', backgroundColor: colors.background },
  content: { flex: 1 },
  loadingScreen: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  loadingTitle: { color: colors.ink, fontSize: 25, fontWeight: '900', marginTop: 14 },
  header: { minHeight: 72, flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 18, paddingTop: 8, paddingBottom: 8 },
  logoMark: { width: 42, height: 42, borderRadius: 14, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.brand },
  logoEmoji: { color: '#FFFFFF', fontSize: 21, fontWeight: '900' },
  brandName: { color: colors.ink, fontSize: 18, fontWeight: '900' },
  brandTagline: { color: colors.muted, fontSize: 11, marginTop: 1 },
  notice: { marginHorizontal: 16, marginBottom: 6, backgroundColor: colors.accentSoft, borderRadius: 13, padding: 11, flexDirection: 'row', gap: 8, alignItems: 'center' },
  noticeText: { flex: 1, color: '#7E391C', fontSize: 13, fontWeight: '600' }, noticeClose: { color: '#7E391C', fontSize: 20 },
  scrollContent: { paddingHorizontal: 16, paddingTop: 8, paddingBottom: 32, gap: 12 },
  hero: { backgroundColor: colors.brand, borderRadius: 26, padding: 22, gap: 14, overflow: 'hidden' },
  heroTitle: { color: '#FFFFFF', fontSize: 34, lineHeight: 38, fontWeight: '900', letterSpacing: -0.7 },
  heroBody: { color: '#DED9EC', fontSize: 15, lineHeight: 22, maxWidth: 610 },
  relaxedButton: { alignItems: 'center', paddingTop: 1 }, relaxedText: { color: '#FFFFFF', fontSize: 13, fontWeight: '700', textDecorationLine: 'underline' },
  statsRow: { flexDirection: 'row', gap: 8 },
  statCard: { flex: 1, padding: 13, borderRadius: 17 }, statValue: { color: colors.ink, fontSize: 25, fontWeight: '900' }, statLabel: { color: colors.muted, fontSize: 11, marginTop: 3 },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 7 },
  sectionTitle: { color: colors.ink, fontSize: 19, fontWeight: '900' }, sectionMeta: { color: colors.muted, fontSize: 12 },
  historyCompact: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 13 },
  historyDot: { width: 10, height: 10, borderRadius: 10, backgroundColor: colors.accent },
  cardTitle: { color: colors.ink, fontSize: 16, fontWeight: '800', flexShrink: 1 }, cardMuted: { color: colors.muted, fontSize: 13, lineHeight: 18, marginTop: 3 },
  emptyTitle: { color: colors.ink, fontSize: 16, fontWeight: '800' },
  previewGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  previewCard: { width: '48%', gap: 10 }, previewName: { color: colors.ink, fontSize: 14, lineHeight: 18, fontWeight: '800', minHeight: 36 },
  titleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  pageTitle: { color: colors.ink, fontSize: 30, fontWeight: '900', letterSpacing: -0.5 }, pageSubtitle: { color: colors.muted, fontSize: 14, marginTop: 3, marginBottom: 8 },
  addCircle: { width: 45, height: 45, borderRadius: 15, backgroundColor: colors.accent, alignItems: 'center', justifyContent: 'center' }, addCircleText: { color: '#FFF', fontSize: 24, fontWeight: '500' },
  searchInput: { backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line, borderRadius: 15, minHeight: 48, paddingHorizontal: 15, fontSize: 15, color: colors.ink },
  mealCard: { gap: 2 }, mealTop: { flexDirection: 'row', gap: 13 }, mealInfo: { flex: 1 }, mealNameRow: { flexDirection: 'row', gap: 8, alignItems: 'flex-start', justifyContent: 'space-between' },
  inlinePills: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 9 },
  actionRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 14 },
  stepper: { flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: colors.line, borderRadius: 12, overflow: 'hidden' },
  stepperButton: { width: 34, height: 36, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  stepperText: { color: colors.brand, fontSize: 18, fontWeight: '800' }, stepperValue: { minWidth: 27, textAlign: 'center', color: colors.ink, fontWeight: '800' },
  smallAction: { backgroundColor: colors.brandSoft, borderRadius: 11, paddingHorizontal: 12, height: 36, justifyContent: 'center' }, smallActionText: { color: colors.brand, fontSize: 12, fontWeight: '800' },
  deleteAction: { marginLeft: 'auto', padding: 8 }, deleteText: { color: colors.danger, fontSize: 12, fontWeight: '700' }, disabled: { opacity: 0.4 },
  photoCard: { gap: 12 }, photoPreview: { width: '100%', height: 230, borderRadius: 17 },
  photoEmpty: { height: 190, backgroundColor: colors.background, borderRadius: 17, alignItems: 'center', justifyContent: 'center', gap: 5, borderStyle: 'dashed', borderWidth: 1, borderColor: '#CBCAC1' },
  photoEmoji: { fontSize: 38 }, photoActions: { flexDirection: 'row', gap: 10 }, photoButton: { flex: 1, minHeight: 44, backgroundColor: colors.brandSoft, borderRadius: 12, alignItems: 'center', justifyContent: 'center' }, photoButtonText: { color: colors.brand, fontWeight: '800' },
  field: { marginBottom: 13 }, fieldCompact: { flex: 1 }, fieldLabel: { color: colors.ink, fontSize: 12, fontWeight: '800', marginBottom: 6 },
  input: { backgroundColor: colors.background, borderWidth: 1, borderColor: colors.line, borderRadius: 12, minHeight: 45, paddingHorizontal: 12, color: colors.ink, fontSize: 15 },
  inputMultiline: { minHeight: 76, paddingTop: 11, textAlignVertical: 'top' }, twoColumns: { flexDirection: 'row', gap: 12 },
  carbCallout: { backgroundColor: colors.mint, borderRadius: 13, padding: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  carbLabel: { color: colors.mintInk, fontSize: 13, fontWeight: '700' }, carbTotal: { color: colors.mintInk, fontSize: 18, fontWeight: '900' },
  formFootnote: { color: colors.muted, fontSize: 11, lineHeight: 16, textAlign: 'center', paddingHorizontal: 12 },
  historyCard: { flexDirection: 'row', alignItems: 'center', gap: 13 },
  calendarBadge: { width: 48, height: 52, borderRadius: 13, backgroundColor: colors.brandSoft, alignItems: 'center', justifyContent: 'center' },
  calendarMonth: { color: colors.brand, fontSize: 9, fontWeight: '900' }, calendarDay: { color: colors.brand, fontSize: 21, fontWeight: '900', lineHeight: 23 },
  historyNutrition: { color: colors.mintInk, fontSize: 12, marginTop: 6, fontWeight: '600' }, undoButton: { padding: 9 }, undoText: { color: colors.accent, fontSize: 12, fontWeight: '800' },
  bottomNav: { minHeight: 70, flexDirection: 'row', backgroundColor: colors.surface, borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 7, paddingBottom: Platform.OS === 'ios' ? 2 : 7 },
  navItem: { flex: 1, alignItems: 'center', gap: 2 }, navIconWrap: { width: 34, height: 30, borderRadius: 11, alignItems: 'center', justifyContent: 'center' },
  navIconActive: { backgroundColor: colors.brandSoft }, navIcon: { color: colors.muted, fontSize: 19, fontWeight: '700' }, navIconTextActive: { color: colors.brand },
  navLabel: { color: colors.muted, fontSize: 10, fontWeight: '700' }, navLabelActive: { color: colors.brand },
  modalBackdrop: { flex: 1, backgroundColor: 'rgba(18,20,26,0.72)', justifyContent: 'center', padding: 18 },
  modalCard: { width: '100%', maxWidth: 540, alignSelf: 'center', backgroundColor: colors.surface, borderRadius: 28, padding: 18, gap: 13 },
  modalEyebrow: { color: colors.accent, fontSize: 11, letterSpacing: 1.6, fontWeight: '900', textAlign: 'center' },
  modalTitle: { color: colors.ink, fontSize: 29, lineHeight: 33, fontWeight: '900', textAlign: 'center' }, modalBody: { color: colors.muted, fontSize: 14, textAlign: 'center' },
  modalPills: { flexDirection: 'row', gap: 8, justifyContent: 'center', flexWrap: 'wrap' },
  inventoryMessage: { backgroundColor: colors.mint, padding: 11, borderRadius: 12 }, inventoryMessageText: { color: colors.mintInk, fontSize: 12, textAlign: 'center', fontWeight: '700' },
  busyOverlay: { position: 'absolute', top: 0, right: 0, bottom: 0, left: 0, backgroundColor: 'rgba(247,247,242,0.35)', alignItems: 'center', justifyContent: 'center' }
});
