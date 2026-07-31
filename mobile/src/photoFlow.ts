/** Physical side of a prepared-meal card. */
export type PhotoSide = 'front' | 'back';

/** Current stage of the two-photo capture and review flow. */
export type PhotoCaptureStep = PhotoSide | 'review';

/** Normalized metadata retained from an Expo image-picker asset. */
export type SelectedPhoto = {
  uri: string;
  base64?: string | null;
  mimeType?: string | null;
  fileName?: string | null;
};

/** Cooking-code values prepared for explicit human review. */
export type CookingCodeReview = {
  frontCookingMealCode: string;
  backCookingMealCode: string;
  cookingMealCode: string;
  conflict: boolean;
};

/**
 * Determines which card side must be captured next.
 *
 * @param frontReady whether the meal-card front is present
 * @param backReady whether the cooking-guide back is present
 * @returns the next capture side or the review stage
 */
export function photoCaptureStep(frontReady: boolean, backReady: boolean): PhotoCaptureStep {
  if (!frontReady) return 'front';
  if (!backReady) return 'back';
  return 'review';
}

/**
 * Converts a selected photo into the matching form-state fields.
 *
 * @param side card side represented by the asset
 * @param asset normalized image-picker asset
 * @returns form fields for the selected card side
 */
export function photoFormPatch(side: PhotoSide, asset: SelectedPhoto) {
  const values = {
    uri: asset.uri,
    base64: asset.base64 ?? undefined,
    mime: asset.mimeType ?? 'image/jpeg',
    name: asset.fileName ?? `${side}-meal-card.jpg`
  };
  return side === 'front'
    ? {
        frontImageUri: values.uri,
        frontImageBase64: values.base64,
        frontImageMime: values.mime,
        frontImageName: values.name
      }
    : {
        backImageUri: values.uri,
        backImageBase64: values.base64,
        backImageMime: values.mime,
        backImageName: values.name
      };
}

/**
 * Reconciles cooking-code candidates read independently from both card sides.
 *
 * A disagreement deliberately leaves the confirmed code blank so OCR cannot
 * silently choose the wrong cooking program.
 *
 * @param frontCode code read from the bottom of the card front
 * @param backCode code read from the cooking-guide back
 * @param proposedCode extraction service's proposed confirmed value
 * @returns candidate values, conflict state, and safe initial review value
 */
export function cookingCodeReview(
  frontCode?: string | null,
  backCode?: string | null,
  proposedCode?: string | null
): CookingCodeReview {
  const frontCookingMealCode = frontCode ?? '';
  const backCookingMealCode = backCode ?? '';
  const conflict = Boolean(
    frontCookingMealCode
      && backCookingMealCode
      && frontCookingMealCode !== backCookingMealCode
  );

  return {
    frontCookingMealCode,
    backCookingMealCode,
    cookingMealCode: conflict
      ? ''
      : proposedCode ?? (frontCookingMealCode || backCookingMealCode),
    conflict
  };
}
