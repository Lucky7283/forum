package az.animeazerbaycan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(columnDefinition = "text")
    private String avatarUrl;
    @Column(nullable = false, length = 20)
    private String role = "USER";
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void create() {
        createdAt = Instant.now();
    }
}
