package az.animeazerbaycan.service.Implementation;

import az.animeazerbaycan.dto.request.LoginRequest;
import az.animeazerbaycan.dto.request.ProfileRequest;
import az.animeazerbaycan.dto.request.RegisterRequest;
import az.animeazerbaycan.dto.response.ProfileResponse;
import az.animeazerbaycan.dto.response.UserResponse;
import az.animeazerbaycan.service.Interface.UserService;

import az.animeazerbaycan.entity.User;
import az.animeazerbaycan.repository.UserRepository;
import az.animeazerbaycan.mapper.ResponseMapper;
import az.animeazerbaycan.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final ResponseMapper mapper;

    @Override
    public UserResponse register(RegisterRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400, "Password must be at most 72 UTF-8 bytes");
        if (users.existsByEmail(email) || users.existsByUsername(request.username()))
            throw new ApiException(409, "Username or email already exists");
        User u = new User();
        u.setUsername(request.username());
        u.setEmail(email);
        u.setPassword(passwords.encode(request.password()));
        return mapper.user(users.saveAndFlush(u));
    }

    @Override
    public UserResponse login(LoginRequest request) {
        User u = users.findByEmail(request.email().strip().toLowerCase(Locale.ROOT)).orElseThrow(() -> new ApiException(401, "Invalid credentials"));
        if (!passwords.matches(request.password(), u.getPassword())) throw new ApiException(401, "Invalid credentials");
        return mapper.user(u);
    }

    @Override
    public User require(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(401, "Authentication required"));
    }

    @Override
    public User lock(Long id) {
        return users.lockById(id).orElseThrow(() -> new ApiException(401, "Authentication required"));
    }

    @Override
    public UserResponse me(Long id) {
        return mapper.user(require(id));
    }

    @Override
    public ProfileResponse profile(String username) {
        return mapper.profile(users.findByUsername(username).orElseThrow(ApiException::missing));
    }

    @Override
    public UserResponse update(Long id, ProfileRequest request) {
        User u = lock(id);
        if (request.getUsername() != null) u.setUsername(request.getUsername());
        if (request.isAvatarProvided()) u.setAvatarUrl(request.getAvatarUrl());
        return mapper.user(users.saveAndFlush(u));
    }
}
