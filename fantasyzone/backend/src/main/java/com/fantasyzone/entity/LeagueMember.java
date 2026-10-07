package com.fantasyzone.entity;

import jakarta.persistence.*;
import lombok.*;

/** Bridge entity for users <-> leagues (many-to-many) that also carries the commissioner flag. */
@Entity
@Table(name = "league_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "league_id"}))
@Getter @Setter @NoArgsConstructor
public class LeagueMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;

    private boolean commissioner = false;
}
