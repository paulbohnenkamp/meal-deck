export type PhotoSide = 'front' | 'back';
export type PhotoCaptureStep = PhotoSide | 'review';

export type SelectedPhoto = {
  uri: string;
  base64?: string | null;
  mimeType?: string | null;
  fileName?: string | null;
};

export function photoCaptureStep(frontReady: boolean, backReady: boolean): PhotoCaptureStep {
  if (!frontReady) return 'front';
  if (!backReady) return 'back';
  return 'review';
}

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
