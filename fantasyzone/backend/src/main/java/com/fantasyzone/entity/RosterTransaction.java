package com.fantasyzone.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Append-only history of roster adds and drops. */
@Entity
@Table(name = "roster_transactions",
       indexes = @Index(name = "idx_tx_team_created", columnList = "fantasy_team_id,created_at"))
@Getter @Setter @NoArgsConstructor
public class RosterTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fantasy_team_id")
    private FantasyTeam fantasyTeam;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "nfl_player_id")
    private NflPlayer nflPlayer;

    /** Who made the move (the owner, or an admin acting for them). */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(length = 10)
    private String positionSlot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
