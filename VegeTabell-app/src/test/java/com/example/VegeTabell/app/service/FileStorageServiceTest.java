package com.example.VegeTabell.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @Test
    void store_savesFileWithJpgExtensionAndReturnsUploadsPath(@TempDir Path tempDir) throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("photo", "tomato.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes());

        String result = service.store(file);

        assertTrue(result.startsWith("/uploads/"));
        assertTrue(result.endsWith(".jpg"));
        Path saved = tempDir.resolve(result.substring("/uploads/".length()));
        assertTrue(Files.exists(saved));
        assertEquals("fake-jpeg-bytes", Files.readString(saved));
    }

    @Test
    void isSupportedImage_acceptsKnownImageTypes(@TempDir Path tempDir) {
        FileStorageService service = new FileStorageService(tempDir.toString());

        assertTrue(service.isSupportedImage(
                new MockMultipartFile("photo", "a.png", "image/png", new byte[0])));
        assertFalse(service.isSupportedImage(
                new MockMultipartFile("photo", "a.txt", "text/plain", new byte[0])));
    }
}
