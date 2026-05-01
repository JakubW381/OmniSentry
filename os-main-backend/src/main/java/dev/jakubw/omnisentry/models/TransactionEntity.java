package dev.jakubw.omnisentry.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_external_id", columnList = "saltEdgeTransactionId"),
        @Index(name = "idx_transaction_account_id", columnList = "saltEdgeAccountId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID internalId;

    @Column(unique = true, nullable = false)
    private String saltEdgeTransactionId;

    @Column(nullable = false)
    private String saltEdgeAccountId;

    @Column(nullable = false)
    private String saltEdgeConnectionId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(nullable = false)
    private LocalDate madeOn;

    @Column(nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> extra = Map.of();

    @Builder.Default
    private boolean isSuspicious = false;

    private String createdAt;
}
