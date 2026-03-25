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
public class CommentResponse {
    String id;
    String authorId;
    String authorProfileId;
    String authorFirstName;
    String authorLastName;
    String authorProfilePicture;
    String storyId;
    String storyPartId;
    String parentCommentId;
    String rootCommentId;
    int depth;
    long likeCount;
    long replyCount;
    String content;
    boolean deleted;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
