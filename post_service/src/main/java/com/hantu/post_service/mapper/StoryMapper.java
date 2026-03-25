package com.hantu.post_service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.hantu.post_service.dto.request.CreateStoryRequest;
import com.hantu.post_service.dto.request.UpdateStoryRequest;
import com.hantu.post_service.dto.response.StoryResponse;
import com.hantu.post_service.entity.Story;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StoryMapper {

    StoryResponse toStoryResponse(Story story);

    Story toStory(CreateStoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateStory(UpdateStoryRequest request, @MappingTarget Story story);

}
