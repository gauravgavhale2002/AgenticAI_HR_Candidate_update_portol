package com.talentflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "blocked_phone_numbers")
public class BlockedPhoneNumber extends BaseEntity {

    @Column(nullable = false, unique = true, length = 16)
    private String phoneNumber;
}
