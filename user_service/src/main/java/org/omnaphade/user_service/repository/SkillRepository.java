package org.omnaphade.user_service.repository;

import org.omnaphade.user_service.entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Skill findById(long id);

    Optional<Skill> findByNameIgnoreCase(String name);
}
