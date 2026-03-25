package com.hantu.identity_service.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import java.time.LocalDate;
import org.hibernate.validator.constraints.URL;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreattionRequest {

    @NotBlank(message = "INVALID_USERNAME")
    @Size(min = 3, max = 30, message = "INVALID_USERNAME")
    String username;

    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 8, max = 20, message = "INVALID_PASSWORD")
    String password;

    @Email(message = "INVALID_EMAIL")
    String email;

    @NotNull(message = "INVALID_GENDER")
    boolean gender;

    @NotBlank(message = "INVALID_LAST_NAME")
    @Size(min = 1, max = 50, message = "INVALID_LAST_NAME")
    String lastName;

    @NotBlank(message = "INVALID_FIRST_NAME")
    @Size(min = 1, max = 50, message = "INVALID_FIRST_NAME")
    String firstName;

    @Size(max = 200, message = "INVALID_BIO")
    String bio;

    @Size(max = 200, message = "INVALID_ADDRESS")
    String address;
    
    @Past(message = "INVALID_DOB")
    LocalDate dob;

    @URL(message = "INVALID_AVATAR_URL")
    String profilePictureUrl;

}
