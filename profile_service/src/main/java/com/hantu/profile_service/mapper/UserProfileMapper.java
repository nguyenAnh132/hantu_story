package com.hantu.profile_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import com.hantu.profile_service.entity.UserProfile;
import com.hantu.profile_service.dto.request.UserProfileCreationRequest;
import com.hantu.profile_service.dto.response.UserProfileResponse;
import com.hantu.profile_service.dto.request.UserProfileUpdateRequest;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    UserProfile toUserProfile(UserProfileCreationRequest request);

    UserProfileResponse toUserProfileResponse(UserProfile userProfile);

    void updateUserProfile(UserProfileUpdateRequest request, @MappingTarget UserProfile userProfile);

}
