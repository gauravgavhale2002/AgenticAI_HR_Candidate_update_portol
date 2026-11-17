package com.talentflow.repository;

import com.talentflow.entity.BlockedPhoneNumber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedPhoneNumberRepository extends JpaRepository<BlockedPhoneNumber, Long> {
    boolean existsByPhoneNumber(String phoneNumber);
}
