package com.emsp.assignment.infrastructure.persistence.card;

import com.emsp.assignment.domain.card.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    // 按账户邮箱查询关联卡
    List<Card> findByAccountEmail(String accountEmail);
}