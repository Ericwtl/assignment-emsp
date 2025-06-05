package com.emsp.assignment.domain.card.model;

import com.emsp.assignment.domain.account.model.Account;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.time.LocalDateTime;
import java.util.UUID; // 新增导入

@Entity
@Table(name = "card")
@Data
public class Card {
    @Id
    @GeneratedValue
    @Column(name = "uid", columnDefinition = "uuid", updatable = false)
    private UUID uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_email", referencedColumnName = "email", foreignKey = @ForeignKey(name = "fk_card_account"))
    private Account account;

    @Lob
    @Column(name = "encrypted_card", nullable = false, columnDefinition = "BYTEA")
    private byte[] encryptedCard;

    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @Generated(GenerationTime.ALWAYS)
    @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}-\\d{4}$")
    @Column(name = "visible_number", insertable = false, updatable = false)
    private String visibleNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status = CardStatus.CREATED;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

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

}