package com.example.wander.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleChangeRequestRepository extends JpaRepository<RoleChangeRequestEntity, Long> {

    Optional<RoleChangeRequestEntity> findByUserIdAndStatus(Long userId, RoleChangeStatus status);

    List<RoleChangeRequestEntity> findByStatus(RoleChangeStatus status);

    List<RoleChangeRequestEntity> findByUserId(Long userId);

    long deleteByUserId(Long userId);
}
