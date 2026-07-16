package dev.jakubw.omnisentry.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountEntity {

    @Id
    @Column(unique = true, nullable = false)
    private String saltEdgeAccountId;

    private String connectionId;
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

    @Override
    public boolean equals(Object obj) {
        return saltEdgeAccountId.equals(((AccountEntity) obj).saltEdgeAccountId);
    }
}
