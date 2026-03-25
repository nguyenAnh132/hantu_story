package com.hantu.post_service.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

@Document("reactions")
@CompoundIndex(name = "reaction_user_target_unique", def = "{'userId': 1, 'targetType': 1, 'targetId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reaction {

    @Id
    String id;
    String userId;
    String targetType;
    String targetId;
    String reactionType;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

}
