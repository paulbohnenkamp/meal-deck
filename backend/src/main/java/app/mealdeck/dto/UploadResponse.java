package app.mealdeck.dto;

/**
 * Public path assigned to a stored image.
 *
 * @param imageUrl application-relative image URL
 */
public record UploadResponse(String imageUrl) {}
