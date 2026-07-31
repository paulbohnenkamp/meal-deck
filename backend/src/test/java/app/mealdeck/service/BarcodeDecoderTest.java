package app.mealdeck.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

/** Verifies deterministic decoding of supported meal-card identifiers. */
class BarcodeDecoderTest {
    private final BarcodeDecoder decoder = new BarcodeDecoder();

    @Test
    void decodesFrontCode128WithoutTreatingItAsQr() throws Exception {
        MockMultipartFile image = image("310012345678", BarcodeFormat.CODE_128, 500, 180);

        assertThat(decoder.decodeFrontBarcode(image)).isEqualTo("310012345678");
        assertThat(decoder.decodeBackQr(image)).isNull();
    }

    @Test
    void decodesBackQrWithoutTreatingItAsFrontBarcode() throws Exception {
        MockMultipartFile image = image(
                "https://suvie.com/m/012-A", BarcodeFormat.QR_CODE, 320, 320);

        assertThat(decoder.decodeBackQr(image)).isEqualTo("https://suvie.com/m/012-A");
        assertThat(decoder.decodeFrontBarcode(image)).isNull();
    }

    private MockMultipartFile image(
            String payload,
            BarcodeFormat format,
            int width,
            int height) throws Exception {
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(
                new MultiFormatWriter().encode(payload, format, width, height));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", bytes);
        return new MockMultipartFile(
                "image", "identifier.png", "image/png", bytes.toByteArray());
    }
}
