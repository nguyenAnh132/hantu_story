package com.hantu.profile_service.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import com.hantu.profile_service.entity.UserProfile;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserProfileRepository extends Neo4jRepository<UserProfile, String> {

    Optional<UserProfile> findByUserId(String userId);

    interface FollowingEdge {
        String getUserId();
        Long getFollowedAtMillis();
    }

    interface FollowerEdge {
        String getUserId();
        Long getFollowedAtMillis();
    }

    @Query("""
            MATCH (f {userId: $followerUserId})-[r:FOLLOWS]->(t)
            RETURN t.userId AS userId, r.createdAt AS followedAtMillis
            ORDER BY r.createdAt DESC, t.userId DESC
            LIMIT $limit
            """)
    List<FollowingEdge> findFollowingEdgesFirstPage(
            @Param("followerUserId") String followerUserId,
            @Param("limit") int limit
    );

    @Query("""
            MATCH (f {userId: $followerUserId})-[r:FOLLOWS]->(t)
            WHERE r.createdAt < $lastFollowedAtMillis
               OR (r.createdAt = $lastFollowedAtMillis AND t.userId < $lastTargetUserId)
            RETURN t.userId AS userId, r.createdAt AS followedAtMillis
            ORDER BY r.createdAt DESC, t.userId DESC
            LIMIT $limit
            """)
    List<FollowingEdge> findFollowingEdgesAfterCursor(
            @Param("followerUserId") String followerUserId,
            @Param("lastFollowedAtMillis") long lastFollowedAtMillis,
            @Param("lastTargetUserId") String lastTargetUserId,
            @Param("limit") int limit
    );

    @Query("""
            MATCH (t {userId: $targetUserId})<-[r:FOLLOWS]-(f)
            RETURN f.userId AS userId, r.createdAt AS followedAtMillis
            ORDER BY r.createdAt DESC, f.userId DESC
            LIMIT $limit
            """)
    List<FollowerEdge> findFollowerEdgesFirstPage(
            @Param("targetUserId") String targetUserId,
            @Param("limit") int limit
    );

    @Query("""
            MATCH (t {userId: $targetUserId})<-[r:FOLLOWS]-(f)
            WHERE r.createdAt < $lastFollowedAtMillis
               OR (r.createdAt = $lastFollowedAtMillis AND f.userId < $lastFollowerUserId)
            RETURN f.userId AS userId, r.createdAt AS followedAtMillis
            ORDER BY r.createdAt DESC, f.userId DESC
            LIMIT $limit
            """)
    List<FollowerEdge> findFollowerEdgesAfterCursor(
            @Param("targetUserId") String targetUserId,
            @Param("lastFollowedAtMillis") long lastFollowedAtMillis,
            @Param("lastFollowerUserId") String lastFollowerUserId,
            @Param("limit") int limit
    );

    @Query("""
            MATCH (f {userId: $followerUserId})
            MATCH (t {userId: $targetUserId})
            MERGE (f)-[r:FOLLOWS]->(t)
            ON CREATE SET r.createdAt = timestamp()
            RETURN r.createdAt
            """)
    Long follow(
            @Param("followerUserId") String followerUserId,
            @Param("targetUserId") String targetUserId
    );

    @Query("""
            MATCH (f {userId: $followerUserId})-[r:FOLLOWS]->(t {userId: $targetUserId})
            DELETE r
            RETURN 1
            """)
    Long unfollow(
            @Param("followerUserId") String followerUserId,
            @Param("targetUserId") String targetUserId
    );

    @Query("""
            MATCH (f {userId: $followerUserId})-[r:FOLLOWS]->(t {userId: $targetUserId})
            RETURN COUNT(r)
            """)
    Long countFollowing(
            @Param("followerUserId") String followerUserId,
            @Param("targetUserId") String targetUserId
    );

    @Query("""
            MATCH (f {userId: $followerUserId})-[r:FOLLOWS]->()
            RETURN COUNT(r)
            """)
    Long countFollowingByFollower(
            @Param("followerUserId") String followerUserId
    );

    @Query("""
            MATCH (t {userId: $targetUserId})<-[r:FOLLOWS]-()
            RETURN COUNT(r)
            """)
    Long countFollowersByTarget(
            @Param("targetUserId") String targetUserId
    );

    @Query("""
            MATCH (u)
            WHERE u.userId IN $userIds
            RETURN u
            """)
    List<UserProfile> findAllByUserIds(@Param("userIds") List<String> userIds);

}
