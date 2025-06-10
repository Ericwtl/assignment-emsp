package com.emsp.assignment.infrastructure.persistence;

import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.model.AccountStatus;
import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private Account activeAccount;
    private Account deactiveAccount;
    private Card activeCard;

    private final String ACTIVE_EMAIL = "active@example.com";
    private final String INACTIVE_EMAIL = "inactive@example.com";

    @BeforeEach
    void setUp() {
        activeAccount = new Account();
        activeAccount.setEmail(ACTIVE_EMAIL);
        activeAccount.setContractId("DE5X9ABCD12345");
        activeAccount.setStatus(AccountStatus.ACTIVATED);
        activeAccount.setLastUpdated(LocalDateTime.now());

        deactiveAccount = new Account();
        deactiveAccount.setEmail(INACTIVE_EMAIL);
        deactiveAccount.setContractId("DE5X9ABCD12345");
        deactiveAccount.setStatus(AccountStatus.DEACTIVATED);
        deactiveAccount.setLastUpdated(LocalDateTime.now().minusDays(10));

        entityManager.persist(activeAccount);
        entityManager.persist(deactiveAccount);

        activeCard = new Card();
        activeCard.setRfidUid("CARD-123");
        activeCard.setVisibleNumber("1234-5678-9012-3456");
        activeCard.setAccount(activeAccount);
        activeCard.setStatus(CardStatus.ACTIVATED);
        //activeCard.setCreatedAt(Instant.now());

        entityManager.persist(activeCard);

        // 刷新并清除缓存
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void testCardCreation() {
        Card testCard = new Card();
        testCard.setRfidUid("TEST-123");
        testCard.setVisibleNumber("1111-2222-3333-4444");
        testCard.setStatus(CardStatus.CREATED);

        entityManager.persist(testCard);
        entityManager.flush();

        assertNotNull(testCard.getCreatedAt());
    }

    // ================= findByEmail 方法测试 =================
    @Test
    void findByEmail_shouldReturnAccountWhenExists() {
        // 执行查询
        Optional<Account> result = accountRepository.findByEmail(ACTIVE_EMAIL);

        // 验证结果
        assertTrue(result.isPresent(), "账户应该存在");
        assertEquals(ACTIVE_EMAIL, result.get().getEmail(), "邮箱应该匹配");
        assertEquals(AccountStatus.ACTIVATED, result.get().getStatus(), "状态应该匹配");
    }

    @Test
    void findByEmail_shouldReturnEmptyWhenNotExists() {
        // 执行查询
        Optional<Account> result = accountRepository.findByEmail("nonexistent@example.com");

        // 验证结果
        assertFalse(result.isPresent(), "账户不应该存在");
    }

    // ================= findWithCardsByLastUpdatedBetween 方法测试 =================
    @Test
    void findWithCardsByLastUpdatedBetween_shouldReturnAccountsWithCards() {
        // 设置查询参数
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<Account> result = accountRepository.findWithCardsByLastUpdatedBetween(
                start, end, pageable);

        // 验证结果
        assertEquals(2, result.getTotalElements(), "应该只找到一个账户");
        Account foundAccount = result.getContent().get(0);
        assertEquals(ACTIVE_EMAIL, foundAccount.getEmail(), "邮箱应该匹配");

        // 验证卡片关联
        assertNotNull(foundAccount.getCards(), "卡片集合不应为null");
        assertEquals(1, foundAccount.getCards().size(), "应该加载卡片");
        assertEquals(activeCard.getRfidUid(), foundAccount.getCards().get(0).getRfidUid(), "卡片ID应该匹配");
    }

    @Test
    void findWithCardsByLastUpdatedBetween_shouldReturnEmptyWhenNoAccounts() {
        // 设置查询参数（时间范围外）
        LocalDateTime start = LocalDateTime.now().minusDays(20);
        LocalDateTime end = LocalDateTime.now().minusDays(15);
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<Account> result = accountRepository.findWithCardsByLastUpdatedBetween(
                start, end, pageable);

        // 验证结果
        assertEquals(0, result.getTotalElements(), "不应该找到任何账户");
    }

    @Test
    void findWithCardsByLastUpdatedBetween_shouldReturnAccountWithoutCards() {
        // 创建没有卡片的账户
        Account noCardAccount = new Account();
        noCardAccount.setEmail("nocard@example.com");
        noCardAccount.setStatus(AccountStatus.ACTIVATED);
        noCardAccount.setLastUpdated(LocalDateTime.now());
        entityManager.persist(noCardAccount);
        entityManager.flush();

        // 设置查询参数
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<Account> result = accountRepository.findWithCardsByLastUpdatedBetween(
                start, end, pageable);

        // 验证结果
        assertEquals(3, result.getTotalElements(), "应该找到两个账户");

        // 找到无卡片账户并验证
        Optional<Account> foundAccount = result.getContent().stream()
                .filter(a -> a.getEmail().equals("nocard@example.com"))
                .findFirst();

        assertTrue(foundAccount.isPresent(), "应该找到无卡账户");
        assertTrue(foundAccount.get().getCards().isEmpty(), "卡片集合应该为空");
    }

    @Test
    void findWithCardsByLastUpdatedBetween_shouldHandlePagination() {
        // 创建多个账户
        for (int i = 1; i <= 15; i++) {
            Account account = new Account();
            account.setEmail("user" + i + "@example.com");
            account.setStatus(AccountStatus.ACTIVATED);
            account.setLastUpdated(LocalDateTime.now().minusHours(i));
            entityManager.persist(account);
        }
        entityManager.flush();

        // 设置查询参数
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        Pageable pageable = PageRequest.of(1, 5); // 第二页，每页5条

        // 执行查询
        Page<Account> result = accountRepository.findWithCardsByLastUpdatedBetween(
                start, end, pageable);

        // 验证结果s
        assertEquals(17, result.getTotalElements(), "所有账户都应该匹配");
        assertEquals(5, result.getContent().size(), "应该返回5条记录");
        assertEquals(1, result.getNumber(), "应该是第1页（0开始）");
    }

    @Test
    void findWithCardsByLastUpdatedBetween_shouldNotReturnAccountsOutsideTimeRange() {
        // 设置查询参数（仅包含过去的数据）
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now().minusDays(11);
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<Account> result = accountRepository.findWithCardsByLastUpdatedBetween(
                start, end, pageable);

        // 验证结果
        assertEquals(0, result.getTotalElements(), "只应该找到inactive账户");
    }

    @Test
    void findWithCardsByLastUpdatedBetween_shouldIncludeCardsOnlyForMatchingAccounts() {
        // 创建卡片指向不匹配时间范围的账户
        Card cardForInactive = new Card();
        cardForInactive.setRfidUid("INACTIVE-CARD");
        cardForInactive.setVisibleNumber("9999-9999-9999-9999");
        cardForInactive.setAccount(deactiveAccount);
        cardForInactive.setStatus(CardStatus.DEACTIVATED);

        entityManager.persist(cardForInactive);
        entityManager.flush();

        // 设置查询参数（只匹配active账户的时间范围）
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<Account> result = accountRepository.findWithCardsByLastUpdatedBetween(
                start, end, pageable);

        // 验证结果
        assertEquals(2, result.getTotalElements(), "只应该找到active账户");
        Account foundAccount = result.getContent().get(0);

        // 验证卡片集合
        assertNotNull(foundAccount.getCards(), "卡片集合不应为null");
        assertEquals(1, foundAccount.getCards().size(), "应该只有一张卡片");
        assertEquals(activeCard.getRfidUid(), foundAccount.getCards().get(0).getRfidUid(), "卡片ID应该匹配");
    }
}