package com.hantu.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {

    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 8, max = 20, message = "INVALID_PASSWORD")
    String oldPassword;

    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 8, max = 20, message = "INVALID_PASSWORD")
    String newPassword;
}
