package com.talentflow.repository;

import com.talentflow.entity.CandidateStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateStatusHistoryRepository extends JpaRepository<CandidateStatusHistory, Long> {
    List<CandidateStatusHistory> findByCandidateIdOrderByChangedAtAsc(Long candidateId);
}
