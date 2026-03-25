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
import java.util.List;

@Document("story_parts")
@CompoundIndex(name = "story_order_idx", def = "{'storyId': 1, 'order': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StoryPart {

    @Id
    String id;

    int order;

    String content;

    List<String> imageUrls;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    String storyId;
}
 