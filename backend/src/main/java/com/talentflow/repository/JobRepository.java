package com.talentflow.repository;

import com.talentflow.entity.Job;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByActiveTrueOrderByCreatedAtDesc();
}
