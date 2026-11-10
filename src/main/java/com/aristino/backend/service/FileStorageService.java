package com.aristino.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Luu anh san pham len o dia cua may chay backend (thu muc "uploads/products" canh pom.xml).
 * Chi nhan anh JPG/PNG/WEBP/GIF. Ten file duoc tao ngau nhien, KHONG dung ten file nguoi dung gui.
 * Anh duoc phuc vu qua duong dan /uploads/** (xem WebConfig).
 */
@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // Dia chi ma trinh duyet dung de tai anh (backend dang chay o cong 8080)
    @Value("${app.upload.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui long chon file anh");
        }
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Chi nhan anh JPG, PNG, WEBP hoac GIF");
        }

        Path dir = Paths.get(uploadDir, "products").toAbsolutePath().normalize();
        String fileName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Khong the luu file anh");
        }

        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/uploads/products/" + fileName;
    }
}