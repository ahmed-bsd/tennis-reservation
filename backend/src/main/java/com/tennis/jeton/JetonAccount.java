package com.tennis.jeton;

import com.tennis.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "jeton_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JetonAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private Double balance = 0.0;
}
