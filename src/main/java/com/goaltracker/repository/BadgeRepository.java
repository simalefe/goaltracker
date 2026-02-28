package com.goaltracker.repository;

import com.goaltracker.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findByConditionType(String conditionType);

    Optional<Badge> findByCode(String code);
}

