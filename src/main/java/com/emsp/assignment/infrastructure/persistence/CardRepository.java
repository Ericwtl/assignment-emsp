package com.emsp.assignment.infrastructure.persistence;

import com.emsp.assignment.domain.card.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    boolean existsByVisibleNumber(String visibleNumber);

    Optional<Card> findByRfidUid(String rfidUid);
    List<Card> findByAccountEmail(String accountEmail);
}