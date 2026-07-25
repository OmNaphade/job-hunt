package org.omnaphade.user_service.repository;

import org.omnaphade.user_service.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);
}