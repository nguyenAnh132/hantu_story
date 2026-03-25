package com.hantu.profile_service.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileCreationRequest {
    @NotBlank(message = "INVALID_FIRST_NAME")
    @Size(min = 1, max = 30, message = "INVALID_FIRST_NAME")
    String firstName;

    @NotBlank(message = "INVALID_LAST_NAME")
    @Size(min = 1, max = 30, message = "INVALID_LAST_NAME")
    String lastName;

    @Size(max = 200, message = "INVALID_BIO")
    String bio;

    @Size(max = 200, message = "INVALID_ADDRESS")
    String address;

    @Size(max = 200, message = "INVALID_PROFILE_PICTURE")
    String profilePicture;

    @NotNull(message = "INVALID_GENDER")
    boolean gender;

    @Past(message = "INVALID_DOB")
    LocalDate dob;

    @NotBlank(message = "INVALID_USER_ID")
    String userId;

}
