import assert from 'node:assert/strict';
import test from 'node:test';
import { cookingCodeReview, photoCaptureStep, photoFormPatch } from './photoFlow';

/** Verifies deterministic two-sided photo capture and form mapping behavior. */
test('advances from the front to the back and then review', () => {
  assert.equal(photoCaptureStep(false, false), 'front');
  assert.equal(photoCaptureStep(true, false), 'back');
  assert.equal(photoCaptureStep(true, true), 'review');
});

/** Ensures front-photo metadata cannot overwrite back-photo form state. */
test('maps the meal-card photo only to front fields', () => {
  const patch = photoFormPatch('front', {
    uri: 'file:///Teriyaki_Salmon_front.HEIC',
    base64: 'front-data',
    mimeType: 'image/heic',
    fileName: 'Teriyaki_Salmon_front.HEIC'
  });

  assert.deepEqual(patch, {
    frontImageUri: 'file:///Teriyaki_Salmon_front.HEIC',
    frontImageBase64: 'front-data',
    frontImageMime: 'image/heic',
    frontImageName: 'Teriyaki_Salmon_front.HEIC'
  });
  assert.ok(!('backImageUri' in patch));
});

/** Ensures back-photo metadata cannot overwrite front-photo form state. */
test('maps the cooking-guide photo only to back fields', () => {
  const patch = photoFormPatch('back', {
    uri: 'file:///Teriyaki_Salmon_back.HEIC',
    mimeType: 'image/heic',
    fileName: 'Teriyaki_Salmon_back.HEIC'
  });

  assert.deepEqual(patch, {
    backImageUri: 'file:///Teriyaki_Salmon_back.HEIC',
    backImageBase64: undefined,
    backImageMime: 'image/heic',
    backImageName: 'Teriyaki_Salmon_back.HEIC'
  });
  assert.ok(!('frontImageUri' in patch));
});

/** Verifies predictable upload metadata when the picker omits optional values. */
test('uses a stable JPEG fallback when picker metadata is missing', () => {
  assert.deepEqual(photoFormPatch('back', { uri: 'file:///back.jpg' }), {
    backImageUri: 'file:///back.jpg',
    backImageBase64: undefined,
    backImageMime: 'image/jpeg',
    backImageName: 'back-meal-card.jpg'
  });
});

/** Accepts a cooking code corroborated by both card sides. */
test('pre-fills a matching cooking code for review', () => {
  assert.deepEqual(cookingCodeReview('S123', 'S123', 'S123'), {
    frontCookingMealCode: 'S123',
    backCookingMealCode: 'S123',
    cookingMealCode: 'S123',
    conflict: false
  });
});

/** Uses the readable side when only one cooking-code candidate was found. */
test('pre-fills the sole readable cooking code', () => {
  assert.equal(cookingCodeReview(undefined, 'B-42').cookingMealCode, 'B-42');
});

/** Prevents conflicting OCR candidates from becoming a confirmed code. */
test('requires manual resolution when cooking codes conflict', () => {
  assert.deepEqual(cookingCodeReview('S123', 'S128', 'S123'), {
    frontCookingMealCode: 'S123',
    backCookingMealCode: 'S128',
    cookingMealCode: '',
    conflict: true
  });
});
