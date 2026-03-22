package com.nlq.repository;

import com.nlq.entity.DbProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Profile Repository
 */
public interface DbProfileRepository extends JpaRepository<DbProfile, Long> {

    Optional<DbProfile> findByProfileName(String profileName);

    boolean existsByProfileName(String profileName);

    void deleteByProfileName(String profileName);
}
