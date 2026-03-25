package com.hantu.profile_service.dto.response;

import lombok.Builder;

@Builder
public record FileUploadResponse(
        String category,
        String userId,
        String fileName,
        String relativePath,
        String contentType,
        long size,
        long usedBytes,
        long maxBytes
) {
}
