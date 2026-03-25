package com.hantu.post_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    USER_PROFILE_NOT_FOUND(1008, "User profile not found", HttpStatus.NOT_FOUND),
    STORY_NOT_FOUND(1009, "Story not found", HttpStatus.NOT_FOUND),
    STORY_PART_NOT_FOUND(1010, "Story part not found", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND(1011, "Comment not found", HttpStatus.NOT_FOUND),
    INVALID_TARGET_TYPE(1012, "Invalid target type", HttpStatus.BAD_REQUEST),
    STORY_PART_ORDER_CONFLICT(1013, "Story part order already exists", HttpStatus.CONFLICT),


    //Validation Error Code
    INVALID_TITLE(2001, "Title must be less than 255 characters", HttpStatus.BAD_REQUEST),
    INVALID_DESCRIPTION(2002, "Description must be less than 1000 characters", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
