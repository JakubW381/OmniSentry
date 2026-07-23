package dev.jakubw.omnisentry.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "saltEdgeCustomerId")
@ToString(exclude = "connections")
@Builder
@Table(name = "omni_user")
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @Column(unique = true, nullable = false)
    private String saltEdgeCustomerId;

    @Column(unique = true)
    private String username;

    private String name;
    private String surname;
    private Instant dateOfBirth;

    @Column(unique = true)
    private String email;

    @Builder.Default
    @OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL,orphanRemoval = true, mappedBy = "user")
    private Set<ConnectionEntity> connections = new HashSet<>();

    public void addConnection(ConnectionEntity connection){
        connections.add(connection);
        connection.setUser(this);
    }
}
