package com.emsp.assignment.domain.card.model;

import com.emsp.assignment.domain.account.model.Account;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "card")
@Data
public class Card {
    @Id
    @Column(name = "rfid_uid ", updatable = false, length = 14)
    private String rfidUid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_email", referencedColumnName = "email")
    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "email" // 使用邮箱作为唯一标识符
    )
    @JsonIdentityReference(alwaysAsId = true) // 关键：仅序列化ID（邮箱）
    private Account account;

    @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}-\\d{4}$")
    @Column(name = "visible_number", updatable = false, nullable = false, length = 19)
    private String visibleNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status = CardStatus.CREATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now(); // 添加默认值

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void activate() {
        if (this.status != CardStatus.ASSIGNED) {
            throw new IllegalStateException("Only assigned cards can be activated");
        }
        this.status = CardStatus.ACTIVATED;
    }

    public void deactivate() {
        if (this.status == CardStatus.DEACTIVATED) {
            throw new IllegalStateException("Card is already deactivated");
        }
        this.status = CardStatus.DEACTIVATED;
    }

    @JsonSetter("account")
    public void setAccountFromEmail(String email) {
        if (email != null && !email.isBlank()) {
            Account acc = new Account();
            acc.setEmail(email);
            this.account = acc;
        } else {
            this.account = null;
        }
    }

}