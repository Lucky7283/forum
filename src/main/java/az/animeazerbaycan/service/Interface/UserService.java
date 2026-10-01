package az.animeazerbaycan.service.Interface;

import az.animeazerbaycan.dto.request.LoginRequest;
import az.animeazerbaycan.dto.request.ProfileRequest;
import az.animeazerbaycan.dto.request.RegisterRequest;
import az.animeazerbaycan.dto.response.ProfileResponse;
import az.animeazerbaycan.dto.response.UserResponse;
import az.animeazerbaycan.entity.User;

public interface UserService {

    UserResponse register(RegisterRequest request);

    UserResponse login(LoginRequest request);

    User require(Long id);

    User lock(Long id);

    UserResponse me(Long id);

    ProfileResponse profile(String username);

    UserResponse update(Long id, ProfileRequest request);
}
