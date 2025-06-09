package com.emsp.assignment.infrastructure.persistence;

import com.emsp.assignment.domain.account.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByEmail(String email);

    @Query("SELECT DISTINCT a FROM Account a " +
            "LEFT JOIN FETCH a.cards c " +
            "WHERE a.lastUpdated BETWEEN :start AND :end " +
            "AND (c IS NULL OR c.account.email = a.email)") // 确保正确关联
    Page<Account> findWithCardsByLastUpdatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    boolean existsByEmail(String email);



}