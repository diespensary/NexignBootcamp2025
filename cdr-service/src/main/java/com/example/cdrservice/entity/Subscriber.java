package com.example.cdrservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Subscriber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 11, nullable = false, unique = true)
    private String msisdn;

    @Column(length = 50, nullable = false)
    private String operator;
}
