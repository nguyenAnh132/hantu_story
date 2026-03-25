package com.hantu.identity_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be from 8 to 20 characters", HttpStatus.BAD_REQUEST),
    INCORRECT_PASSWORD(1003, "Incorrect password", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1009, "Invalid token", HttpStatus.UNAUTHORIZED),
    ROLE_NOT_FOUND(10010, "Role not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1011, "User not found", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(1012, "Email already exists", HttpStatus.BAD_REQUEST),
    PROFILE_CREATION_FAILED(1013, "Profile creation failed", HttpStatus.BAD_REQUEST),


    //Validation Error Code
    INVALID_USERNAME(2001, "Username must be from 3 to 30 characters", HttpStatus.BAD_REQUEST),
    INVALID_ADDRESS(2002, "Address must be less than 200 characters", HttpStatus.BAD_REQUEST),
    INVALID_PROFILE_PICTURE_URL(2003, "Profile picture URL must be a valid URL", HttpStatus.BAD_REQUEST),
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
