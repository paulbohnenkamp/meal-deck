import React, { PropsWithChildren } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { colors } from './theme';
import { Meal } from './types';

export function Card({ children, style }: PropsWithChildren<{ style?: object }>) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function Pill({ children, tone = 'brand' }: PropsWithChildren<{ tone?: 'brand' | 'mint' | 'orange' | 'neutral' }>) {
  return <View style={[styles.pill, styles[`pill_${tone}`]]}><Text style={[styles.pillText, styles[`pillText_${tone}`]]}>{children}</Text></View>;
}

export function PrimaryButton({ label, onPress, disabled = false, secondary = false }: { label: string; onPress: () => void; disabled?: boolean; secondary?: boolean }) {
  return (
    <Pressable disabled={disabled} onPress={onPress} style={({ pressed }) => [
      styles.button, secondary && styles.buttonSecondary, disabled && styles.buttonDisabled, pressed && !disabled && styles.pressed
    ]}>
      <Text style={[styles.buttonText, secondary && styles.buttonSecondaryText]}>{label}</Text>
    </Pressable>
  );
}

export function NutritionRow({ meal }: { meal: Meal }) {
  const items = [
    ['Carbs', meal.carbsPerServing, 'g'],
    ['Calories', meal.caloriesPerServing, ''],
    ['Protein', meal.proteinPerServing, 'g'],
    ['Fat', meal.fatPerServing, 'g']
  ];
  return (
    <View style={styles.nutritionRow}>
      {items.map(([label, value, suffix]) => (
        <View key={String(label)} style={styles.nutritionItem}>
          <Text style={styles.nutritionValue}>{value ?? '—'}{value != null ? suffix : ''}</Text>
          <Text style={styles.nutritionLabel}>{label}</Text>
        </View>
      ))}
    </View>
  );
}

export function MealImage({ meal, large = false }: { meal: Meal; large?: boolean }) {
  if (meal.imageUrl) return <Image source={{ uri: meal.imageUrl }} style={[styles.image, large && styles.imageLarge]} />;
  return (
    <View style={[styles.image, styles.placeholder, large && styles.imageLarge]}>
      <Text style={styles.placeholderEmoji}>🍽️</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface, borderRadius: 20, padding: 16, borderWidth: 1, borderColor: colors.line,
    shadowColor: colors.shadow, shadowOpacity: 0.06, shadowRadius: 14, shadowOffset: { width: 0, height: 6 }, elevation: 2
  },
  pill: { alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 5, borderRadius: 999 },
  pill_brand: { backgroundColor: colors.brandSoft }, pill_mint: { backgroundColor: colors.mint },
  pill_orange: { backgroundColor: colors.accentSoft }, pill_neutral: { backgroundColor: '#ECEDEB' },
  pillText: { fontSize: 12, fontWeight: '700' }, pillText_brand: { color: colors.brand },
  pillText_mint: { color: colors.mintInk }, pillText_orange: { color: colors.accent }, pillText_neutral: { color: colors.muted },
  button: { minHeight: 50, borderRadius: 14, backgroundColor: colors.accent, justifyContent: 'center', alignItems: 'center', paddingHorizontal: 18 },
  buttonSecondary: { backgroundColor: colors.brandSoft }, buttonDisabled: { opacity: 0.45 }, pressed: { transform: [{ scale: 0.985 }], opacity: 0.9 },
  buttonText: { color: '#FFFFFF', fontSize: 16, fontWeight: '800' }, buttonSecondaryText: { color: colors.brand },
  nutritionRow: { flexDirection: 'row', marginTop: 14, paddingTop: 14, borderTopWidth: 1, borderTopColor: colors.line },
  nutritionItem: { flex: 1 }, nutritionValue: { color: colors.ink, fontSize: 15, fontWeight: '800' },
  nutritionLabel: { color: colors.muted, fontSize: 11, marginTop: 2 },
  image: { width: 76, height: 76, borderRadius: 15, backgroundColor: colors.brandSoft },
  imageLarge: { width: '100%', height: 210, borderRadius: 20 },
  placeholder: { justifyContent: 'center', alignItems: 'center' }, placeholderEmoji: { fontSize: 30 }
});
