package com.talentflow.repository;

import com.talentflow.entity.User;
import com.talentflow.enums.RoleName;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findDistinctByRoles_NameOrderByCreatedAtDesc(RoleName name);
}
