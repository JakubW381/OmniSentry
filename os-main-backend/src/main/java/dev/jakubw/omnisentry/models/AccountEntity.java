package dev.jakubw.omnisentry.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@EqualsAndHashCode(of = "saltEdgeAccountId")
@ToString(exclude = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountEntity {

    @Id
    @Column(unique = true, nullable = false)
    private String saltEdgeAccountId;

    private String name;
    private BigDecimal balance;
    private String currency;
    private String nature;

    private String iban;
    private String bban;
    private String holderName;
    private String status;

    private String createdAt;
    private String updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_saltEdgeConnectionId")
    private ConnectionEntity connection;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY,orphanRemoval = true, mappedBy = "account")
    @Builder.Default
    private List<TransactionEntity> transactions = new ArrayList<>();

    public void addTransaction(TransactionEntity transaction){
        transactions.add(transaction);
        transaction.setAccount(this);
    }
}
