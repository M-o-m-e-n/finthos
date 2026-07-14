package com.alahly.momkn.finthos.user.mapper;

import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.web.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
