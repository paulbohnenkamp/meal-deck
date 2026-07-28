import assert from 'node:assert/strict';
import test from 'node:test';
import { photoCaptureStep, photoFormPatch } from './photoFlow';

test('advances from the front to the back and then review', () => {
  assert.equal(photoCaptureStep(false, false), 'front');
  assert.equal(photoCaptureStep(true, false), 'back');
  assert.equal(photoCaptureStep(true, true), 'review');
});

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

test('uses a stable JPEG fallback when picker metadata is missing', () => {
  assert.deepEqual(photoFormPatch('back', { uri: 'file:///back.jpg' }), {
    backImageUri: 'file:///back.jpg',
    backImageBase64: undefined,
    backImageMime: 'image/jpeg',
    backImageName: 'back-meal-card.jpg'
  });
});
