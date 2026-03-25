package com.hantu.post_service.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateStoryRequest {

    @NotNull(message = "INVALID_TITLE")
    @Size(max = 255, message = "INVALID_TITLE")
    String title;

    @NotNull
    @Size(max = 1000, message = "INVALID_DESCRIPTION")
    String description;
    String status; // PUBLISHED, DRAFT, DELETED
}
