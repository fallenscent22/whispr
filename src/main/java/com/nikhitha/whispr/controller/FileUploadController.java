package com.nikhitha.whispr.controller;

import com.nikhitha.whispr.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {
    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            Authentication authentication) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Validate file size (5MB max)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 5MB"));
            }

            // Validate file type
            if (!isValidFileType(file.getContentType(), type)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type"));
            }

            String folder = "users/" + authentication.getName() + "/" + type;
            FileStorageService.FileUploadResponse response = fileStorageService.uploadFile(file, folder);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("message", "File uploaded successfully");
            apiResponse.put("fileUrl", response.getFileUrl());
            apiResponse.put("fileName", file.getOriginalFilename());
            apiResponse.put("fileSize", file.getSize());
            apiResponse.put("contentType", file.getContentType());
            apiResponse.put("fileKey", response.getFileKey()); // used for deletion

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "File upload failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam("fileKey") String fileKey,
            Authentication authentication) {
        try {
            // Optional: Verify the file belongs to the user
            if (!fileKey.startsWith("users/" + authentication.getName() + "/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Not authorized to delete this file"));
            }

            fileStorageService.deleteFile(fileKey);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "File deletion failed: " + e.getMessage()));
        }
    }

    private boolean isValidFileType(String contentType, String type) {
        return switch (type) {
            case "image" -> contentType != null && contentType.startsWith("image/");
            case "document" -> contentType != null && (contentType.startsWith("application/pdf") ||
                    contentType.equals("application/msword") ||
                    contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                    contentType.startsWith("text/"));
            case "audio" -> contentType != null && contentType.startsWith("audio/");
            case "video" -> contentType != null && contentType.startsWith("video/");
            default -> false;
        };
    }
}
