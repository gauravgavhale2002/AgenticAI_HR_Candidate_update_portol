package com.talentflow.repository;

import com.talentflow.entity.Resume;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findFirstByCandidateIdOrderByUploadedAtDesc(Long candidateId);
}
