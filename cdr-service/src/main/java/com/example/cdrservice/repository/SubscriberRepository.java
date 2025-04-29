package com.example.cdrservice.repository;

import com.example.cdrservice.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    List<Subscriber> findByOperator(String operator);
}
