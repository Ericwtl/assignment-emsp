package com.emsp.assignment.domain.account.model;

import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "account")
public class Account {
    @Id
    @NotBlank
    @Column(name = "email", unique = true, nullable = false)
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private String email; // Primary key

    @Column(name = "contract_id", nullable = false)
    @Pattern(regexp = "^[A-Z]{2}[0-9A-Z]{3}[0-9A-Z]{9}$")
    private String contractId; // EMAID

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.CREATED;

    @Column(name = "last_updated", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        this.lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> cards = new ArrayList<>();

    // 领域行为：激活账户
    public void activate() {
        if (this.status == AccountStatus.ACTIVATED) {
            throw new IllegalStateException("Account is already activated");
        }
        if (this.status != AccountStatus.DEACTIVATED) {
            throw new IllegalStateException("Only deactivated accounts can be activated");
        }
        this.status = AccountStatus.ACTIVATED;
    }

    // 领域行为：停用账户
    public void deactivate() {
        if (this.status == AccountStatus.DEACTIVATED) {
            throw new IllegalStateException("Account is already deactivated");
        }
        this.status = AccountStatus.DEACTIVATED;
    }

    // 安全添加卡片方法
    public void addCard(Card card) {
        cards.add(card);
        card.setAccount(this);
        card.setStatus(CardStatus.ASSIGNED);
        cards.add(card);
    }
}