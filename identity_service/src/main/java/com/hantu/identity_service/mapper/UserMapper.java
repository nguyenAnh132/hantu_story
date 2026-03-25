package com.hantu.identity_service.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.hantu.identity_service.dto.response.UserResponse;
import com.hantu.identity_service.entity.User;
import com.hantu.identity_service.dto.request.UserCreattionRequest;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "active", source = "active")
    UserResponse toUserResponse(User user);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "active", constant = "true")
    User toUser(UserCreattionRequest userCreattionRequest);

}
