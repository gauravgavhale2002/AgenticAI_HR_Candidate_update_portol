package com.talentflow.repository;

import com.talentflow.entity.Candidate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    Optional<Candidate> findByEmail(String email);
    boolean existsByEmail(String email);
}
