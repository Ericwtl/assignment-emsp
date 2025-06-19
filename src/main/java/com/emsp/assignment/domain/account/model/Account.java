package com.emsp.assignment.domain.account.model;

import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @Version
    @Column(columnDefinition = "INT NOT NULL DEFAULT 0")
    private Long version = 0L;

    @Id
    @NotBlank(message = "Email is required")
    @Column(name = "email", unique = true, nullable = false)
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
    private String email; // Primary key

    @Column(name = "contract_id", nullable = true)
    @Pattern(regexp = "^(|([A-Z]{2}[0-9A-Z]{3}[0-9A-Z]{9}))$",
            message = "Contract ID must comply with the EMAID standard or be empty.")
    private String contractId; // EMAID

    @Column(name = "status", nullable = true)
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

    @JsonIgnoreProperties("account")
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> cards = new ArrayList<>();

    public void activate() {
        if (this.status == AccountStatus.ACTIVATED) {
            throw new IllegalStateException("Account is already activated");
        }
        if (this.status != AccountStatus.DEACTIVATED) {
            throw new IllegalStateException("Only deactivated accounts can be activated");
        }
        this.status = AccountStatus.ACTIVATED;
    }

    public void deactivate() {
        if (this.status == AccountStatus.DEACTIVATED) {
            throw new IllegalStateException("Account is already deactivated");
        }
        this.status = AccountStatus.DEACTIVATED;
    }

    public void addCard(Card card) {
        cards.add(card);
        card.setAccount(this);
        card.setStatus(CardStatus.ASSIGNED);
        cards.add(card);
    }
}