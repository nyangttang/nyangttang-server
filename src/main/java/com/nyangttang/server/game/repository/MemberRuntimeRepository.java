package com.nyangttang.server.game.repository;

import com.nyangttang.server.game.entity.MemberRuntime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRuntimeRepository
        extends JpaRepository<MemberRuntime, Long> {
}