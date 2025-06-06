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

    // 按更新时间范围查询账户（分页）
    @Query("SELECT a FROM Account a WHERE a.lastUpdated BETWEEN :start AND :end")
    Page<Account> findByLastUpdatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );
    // 获取账户及其关联卡（避免N+1问题）
//    @Query("SELECT DISTINCT a FROM Account a LEFT JOIN FETCH a.cards WHERE a.lastUpdated BETWEEN :start AND :end")
//    Page<Account> findWithCardsByLastUpdatedBetween(
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end,
//            Pageable pageable
//    );

//    @Query("SELECT a FROM Account a " +
//            "WHERE a.lastUpdated BETWEEN :start AND :end")
//    @EntityGraph(attributePaths = {"cards"}) // 使用@EntityGraph代替JOIN FETCH
//    Page<Account> findWithCardsByLastUpdatedBetween(
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end,
//            Pageable pageable
//    );

    @Query("SELECT DISTINCT a FROM Account a " +
            "LEFT JOIN FETCH a.cards c " +
            "WHERE a.lastUpdated BETWEEN :start AND :end " +
            "AND (c IS NULL OR c.account.email = a.email)") // 确保正确关联
    Page<Account> findWithCardsByLastUpdatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );




}