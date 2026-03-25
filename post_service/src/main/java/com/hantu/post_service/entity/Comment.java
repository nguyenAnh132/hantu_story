package com.hantu.post_service.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import java.time.LocalDateTime;

@Document("comments")
@CompoundIndex(name = "story_part_parent_created_idx", def = "{'storyPartId': 1, 'parentCommentId': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comment {

    @Id
    String id;

    String authorId;
    String storyId;
    String storyPartId;
    String parentCommentId;
    String rootCommentId;

    int depth;
    long likeCount;
    long replyCount;

    String content;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    boolean isDeleted;

}
