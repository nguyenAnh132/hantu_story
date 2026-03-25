package com.hantu.file_service.controller;

import com.hantu.file_service.dto.response.ApiResponse;
import com.hantu.file_service.dto.response.FileDeleteResponse;
import com.hantu.file_service.dto.response.FileUploadResponse;
import com.hantu.file_service.service.FileStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileController {
    FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @RequestParam String category,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.<FileUploadResponse>builder()
                .message("File uploaded successfully")
                .result(fileStorageService.upload(category, file))
                .build());
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<FileDeleteResponse>> delete(
            @RequestParam String category,
            @RequestParam String fileName) {
        return ResponseEntity.ok(ApiResponse.<FileDeleteResponse>builder()
                .message("File deleted successfully")
                .result(fileStorageService.delete(category, fileName))
                .build());
    }

    @GetMapping("/public")
    public ResponseEntity<Resource> readPublic(@RequestParam String path) {
        var fileReadResult = fileStorageService.readPublic(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentType(fileReadResult.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : fileReadResult.contentType())
                .body(fileReadResult.resource());
    }
}
