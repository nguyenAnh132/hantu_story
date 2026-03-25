package com.hantu.profile_service.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;  
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.neo4j.core.schema.Property;


@Node("user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfile {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;

    String firstName;
    String lastName;
    String bio;
    String address;
    String profilePicture;
    boolean gender;
    LocalDate dob;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @Property("userId")
    String userId;

}
