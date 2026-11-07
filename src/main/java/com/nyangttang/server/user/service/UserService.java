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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        User user = new User(request.nickname());

        User savedUser = userRepository.save(user);

        return CreateUserResponse.from(savedUser);
    }
}