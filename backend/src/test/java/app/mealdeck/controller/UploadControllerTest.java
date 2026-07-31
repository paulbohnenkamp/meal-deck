package app.mealdeck.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** Verifies upload validation and filesystem storage behavior. */
class UploadControllerTest {
    @TempDir
    Path uploadDirectory;

    @Test
    void acceptsFrontAndBackHeicPhotos() throws Exception {
        var controller = new UploadController(uploadDirectory.toString());
        var front = new MockMultipartFile(
                "file", "Teriyaki_Salmon_front.HEIC", "image/heic", new byte[] {1, 2, 3});
        var back = new MockMultipartFile(
                "file", "Teriyaki_Salmon_back.HEIC", "image/heic", new byte[] {4, 5, 6});

        var frontResponse = controller.upload(front);
        var backResponse = controller.upload(back);

        assertThat(frontResponse.imageUrl()).endsWith(".heic");
        assertThat(backResponse.imageUrl()).endsWith(".heic");
        assertThat(frontResponse.imageUrl()).isNotEqualTo(backResponse.imageUrl());
        assertThat(uploadDirectory).isDirectoryContaining("glob:**.heic");
        try (var uploadedFiles = Files.list(uploadDirectory)) {
            assertThat(uploadedFiles).hasSize(2);
        }
    }
}
