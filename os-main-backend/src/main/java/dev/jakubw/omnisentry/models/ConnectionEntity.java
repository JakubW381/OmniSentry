package dev.jakubw.omnisentry.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID internalId;

    @Column(unique = true, nullable = false)
    private String saltEdgeConnectionId;

    private String customerId;
    private String providerName;
    private String providerCode;
    private String status;

    private String lastDeviceType;
    private String lastRemoteIp;

    private String createdAt;
}
