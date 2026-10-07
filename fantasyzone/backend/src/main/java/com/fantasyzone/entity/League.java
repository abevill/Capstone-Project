package com.fantasyzone.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Entity @Table(name = "leagues")
@Getter @Setter @NoArgsConstructor
public class League {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 12)
    private String joinCode;

    private int season;
    private boolean rostersLocked = false;

    @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeagueMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FantasyTeam> teams = new ArrayList<>();

    // One league has many matchups
    @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Matchup> matchups = new ArrayList<>();
}
