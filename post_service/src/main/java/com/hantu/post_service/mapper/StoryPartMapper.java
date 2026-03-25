package com.hantu.post_service.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.hantu.post_service.dto.request.CreateStoryPartRequest;
import com.hantu.post_service.dto.request.UpdateStoryPartRequest;
import com.hantu.post_service.dto.response.StoryPartResponse;
import com.hantu.post_service.entity.StoryPart;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StoryPartMapper {

    StoryPartResponse toStoryPartResponse(StoryPart part);

    List<StoryPartResponse> toStoryPartResponses(List<StoryPart> parts);

    StoryPart toStoryPart(CreateStoryPartRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateStoryPart(UpdateStoryPartRequest request, @MappingTarget StoryPart part);
}
