package com.hantu.file_service.model;

import com.hantu.file_service.exception.AppException;
import com.hantu.file_service.exception.ErrorCode;

import java.util.Arrays;

public enum FileCategory {
    AVATAR("avatar", 30L * 1024 * 1024),
    POST_IMAGES("post_images", 1024L * 1024 * 1024),
    STORY_IMAGES("story_images", 1024L * 1024 * 1024);

    private final String folder;
    private final long maxBytes;

    FileCategory(String folder, long maxBytes) {
        this.folder = folder;
        this.maxBytes = maxBytes;
    }

    public String folder() {
        return folder;
    }

    public long maxBytes() {
        return maxBytes;
    }

    public static FileCategory from(String raw) {
        return Arrays.stream(values())
                .filter(value -> value.folder.equalsIgnoreCase(raw))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_FILE_CATEGORY));
    }
}
