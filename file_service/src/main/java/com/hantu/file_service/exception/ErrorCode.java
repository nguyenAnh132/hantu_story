package com.hantu.file_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_FILE_CATEGORY(1014, "Invalid file category", HttpStatus.BAD_REQUEST),
    EMPTY_FILE(1015, "File is empty", HttpStatus.BAD_REQUEST),
    INVALID_FILE_NAME(1016, "Invalid file name", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(1020, "Invalid file type", HttpStatus.BAD_REQUEST),
    STORAGE_IO_ERROR(1017, "Storage operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND(1018, "File not found", HttpStatus.NOT_FOUND),
    STORAGE_QUOTA_EXCEEDED(1019, "Storage quota exceeded", HttpStatus.PAYLOAD_TOO_LARGE);

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
