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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID internalId;

    @Column(unique = true, nullable = false)
    private String saltEdgeAccountId;

    private String connectionId;
    private String name;
    private BigDecimal balance;
    private String currency;
    private String nature;

    private String iban;
    private String holderName;
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rawExtra;

    private String createdAt;
    private String updatedAt;
}
