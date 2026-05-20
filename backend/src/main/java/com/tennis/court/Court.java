package com.tennis.court;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer number;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}
