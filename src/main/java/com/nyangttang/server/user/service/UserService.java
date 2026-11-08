package com.nyangttang.server.user.service;

import com.nyangttang.server.user.dto.CreateUserRequest;
import com.nyangttang.server.user.dto.CreateUserResponse;
import com.nyangttang.server.user.entity.User;
import com.nyangttang.server.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(
            UserRepository userRepository,
            AuthService authService
    ) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        User user = new User(request.nickname());
        User savedUser = userRepository.save(user);

        AuthService.IssuedSession session =
                authService.issueSession(savedUser);

        return new CreateUserResponse(
                savedUser.getId(),
                savedUser.getNickname(),
                session.accessToken(),
                session.expiresAt()
        );
    }
}