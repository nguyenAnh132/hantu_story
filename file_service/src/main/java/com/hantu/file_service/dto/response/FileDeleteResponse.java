package com.hantu.file_service.dto.response;

import lombok.Builder;

@Builder
public record FileDeleteResponse(
        String category,
        String userId,
        String fileName,
        String relativePath,
        long usedBytes,
        long maxBytes
) {
}
