package com.hantu.post_service.dto.response;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StoryResponse {
    String id;
    String title;
    String description;
    String status;
    String authorId;
    String authorProfileId;
    String authorFirstName;
    String authorLastName;
    String authorProfilePicture;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
