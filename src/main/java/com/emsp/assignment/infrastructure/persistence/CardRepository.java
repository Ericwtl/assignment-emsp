package com.emsp.assignment.infrastructure.persistence;

import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    // 检查 visibleNumber 是否已存在
    boolean existsByVisibleNumber(String visibleNumber);

    // 使用rfidUid作为主键
    Optional<Card> findByRfidUid(String rfidUid);
    List<Card> findByAccountEmail(String accountEmail);
    List<Card> findByStatus(CardStatus status);
}