package app.mealdeck.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

/**
 * Decodes machine-readable identifiers directly from submitted meal-card
 * images without sending their payloads to another service.
 */
@Component
public class BarcodeDecoder {
    private static final List<BarcodeFormat> ONE_DIMENSIONAL_FORMATS = List.of(
            BarcodeFormat.CODABAR,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.EAN_8,
            BarcodeFormat.EAN_13,
            BarcodeFormat.ITF,
            BarcodeFormat.RSS_14,
            BarcodeFormat.RSS_EXPANDED,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.UPC_EAN_EXTENSION);

    /**
     * Decodes a one-dimensional barcode from the meal-card front.
     *
     * @param image uploaded meal-card front
     * @return exact decoded payload, or {@code null} when no supported barcode
     *         is readable
     */
    public String decodeFrontBarcode(MultipartFile image) {
        return decode(image, ONE_DIMENSIONAL_FORMATS);
    }

    /**
     * Decodes a QR code from the cooking-guide back.
     *
     * @param image uploaded cooking-guide back
     * @return exact decoded payload, or {@code null} when no QR code is readable
     */
    public String decodeBackQr(MultipartFile image) {
        return decode(image, List.of(BarcodeFormat.QR_CODE));
    }

    private String decode(MultipartFile image, List<BarcodeFormat> formats) {
        try {
            BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
            if (bufferedImage == null) return null;
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            String result = decode(source, hints);
            return result != null ? result : decode(source.invert(), hints);
        } catch (IOException ex) {
            return null;
        }
    }

    private String decode(LuminanceSource source, Map<DecodeHintType, Object> hints) {
        MultiFormatReader reader = new MultiFormatReader();
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = reader.decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException ex) {
            return null;
        } finally {
            reader.reset();
        }
    }
}
