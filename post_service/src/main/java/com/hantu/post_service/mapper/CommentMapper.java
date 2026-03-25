package com.hantu.post_service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.hantu.post_service.dto.request.CreateCommentRequest;
import com.hantu.post_service.dto.response.CommentResponse;
import com.hantu.post_service.entity.Comment;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(target = "deleted", source = "deleted")
    CommentResponse toCommentResponse(Comment comment);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "content", source = "content")
    @Mapping(target = "storyId", source = "storyId")
    @Mapping(target = "storyPartId", source = "storyPartId")
    @Mapping(target = "parentCommentId", source = "parentCommentId")
    Comment toComment(CreateCommentRequest request);
}
