package com.hantu.post_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.hantu.post_service.dto.response.ReactionResponse;
import com.hantu.post_service.entity.Reaction;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReactionMapper {

    ReactionResponse toReactionResponse(Reaction reaction);
}
