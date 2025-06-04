package com.emsp.assignment.account.domain;

import com.emsp.assignment.card.domain.Card;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Account {
    @Id
    private String email; // 主键，邮箱作为标识

    private String contractId; // EMAID格式

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Card> cards = new ArrayList<>();
}