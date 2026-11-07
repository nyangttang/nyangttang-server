package com.nyangttang.server.user.repository;

import com.nyangttang.server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}