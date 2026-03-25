package com.hantu.profile_service.exception;

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
    FOLLOWING_SELF(2010, "Cannot follow yourself", HttpStatus.BAD_REQUEST),
    INVALID_TARGET_USER_ID(2011, "Invalid target user id", HttpStatus.BAD_REQUEST),
    TOO_MANY_USER_IDS(2012, "Too many user ids in batch request", HttpStatus.BAD_REQUEST),
    FILE_SERVICE_ERROR(2013, "File service error", HttpStatus.BAD_GATEWAY),


    //Validation Error Code
    INVALID_FIRST_NAME(2001, "Invalid first name", HttpStatus.BAD_REQUEST),
    INVALID_LAST_NAME(2002, "Invalid last name", HttpStatus.BAD_REQUEST),
    INVALID_BIO(2003, "Invalid bio", HttpStatus.BAD_REQUEST),
    INVALID_ADDRESS(2004, "Invalid address", HttpStatus.BAD_REQUEST),
    INVALID_PROFILE_PICTURE(2005, "Invalid profile picture", HttpStatus.BAD_REQUEST),
    INVALID_GENDER(2006, "Invalid gender", HttpStatus.BAD_REQUEST),
    INVALID_DOB(2007, "Invalid date of birth", HttpStatus.BAD_REQUEST),
    INVALID_USER_ID(2008, "Invalid user id", HttpStatus.BAD_REQUEST),
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
