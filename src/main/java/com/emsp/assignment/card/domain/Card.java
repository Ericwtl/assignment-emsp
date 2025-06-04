package com.emsp.assignment.card.domain;

import com.emsp.assignment.account.domain.Account;
import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID; // 新增导入

@Entity
@Data
public class Card {
    @Id
    @Column(columnDefinition = "UUID") // 明确指定数据库类型
    private UUID uid;

    private String visibleNumber;

    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @ManyToOne
    @JoinColumn(
            name = "account_email",
            referencedColumnName = "email",
            foreignKey = @ForeignKey(name = "fk_card_account")
    )
    private Account account;
}