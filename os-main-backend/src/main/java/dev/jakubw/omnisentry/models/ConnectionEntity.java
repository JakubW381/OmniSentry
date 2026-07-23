package dev.jakubw.omnisentry.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "accounts")
@EqualsAndHashCode(of = "saltEdgeConnectionId")
public class ConnectionEntity {

    @Id
    @Column(unique = true, nullable = false)
    private String saltEdgeConnectionId;

    private String providerName;
    private String providerCode;
    private String status;

    private String lastDeviceType;
    private String lastRemoteIp;

    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_saltEdgeCustomerId")
    private UserEntity user;

    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<AccountEntity> accounts = new HashSet<>();

    public void addAccount(AccountEntity account){
        accounts.add(account);
        account.setConnection(this);
    }
}
