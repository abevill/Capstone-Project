package com.fantasyzone.service;

import com.fantasyzone.dto.RosterEntryResponse;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RosterService {
    static final int MAX_ROSTER_SIZE = 15;

    private final FantasyTeamRepository teams;
    private final NflPlayerRepository players;
    private final RosterPlayerRepository roster;
    private final RosterTransactionRepository transactions;
    private final LeagueAccess access;

    public RosterService(FantasyTeamRepository teams, NflPlayerRepository players, RosterPlayerRepository roster,
                         RosterTransactionRepository transactions, LeagueAccess access) {
        this.teams = teams; this.players = players; this.roster = roster;
        this.transactions = transactions; this.access = access;
    }

    @Transactional
    public RosterEntryResponse addPlayer(Long teamId, Long playerId, String slot, String email) {
        FantasyTeam team = loadEditableTeam(teamId, email);
        NflPlayer player = players.findById(playerId)
            .orElseThrow(() -> new NotFoundException("Player not found"));
        if (roster.isPlayerTakenInLeague(team.getLeague().getId(), playerId))
            throw new ConflictException("Player is already on a team in this league");
        if (roster.findByFantasyTeamId(teamId).size() >= MAX_ROSTER_SIZE)
            throw new ConflictException("Roster is full (" + MAX_ROSTER_SIZE + " players)");

        RosterPlayer spot = new RosterPlayer();
        spot.setFantasyTeam(team);
        spot.setNflPlayer(player);
        spot.setPositionSlot(slot.trim().toUpperCase());
        RosterPlayer saved = roster.save(spot);
        log(team, player, TransactionType.ADD, saved.getPositionSlot(), email);
        return toEntry(saved);
    }

    @Transactional
    public void dropPlayer(Long teamId, Long playerId, String email) {
        FantasyTeam team = loadEditableTeam(teamId, email);
        RosterPlayer spot = roster.findByFantasyTeamIdAndNflPlayerId(teamId, playerId)
            .orElseThrow(() -> new NotFoundException("Player is not on this roster"));
        roster.delete(spot);
        log(team, spot.getNflPlayer(), TransactionType.DROP, spot.getPositionSlot(), email);
    }

    /** Owner (or admin) only, and only while the commissioner has not locked rosters. */
    private FantasyTeam loadEditableTeam(Long teamId, String email) {
        FantasyTeam team = teams.findById(teamId).orElseThrow(() -> new NotFoundException("Team not found"));
        boolean owner = team.getOwner().getEmail().equalsIgnoreCase(email);
        if (!owner && !access.isAdmin(email))
            throw new ForbiddenException("You can only change your own roster");
        if (team.getLeague().isRostersLocked())
            throw new ConflictException("Roster moves are locked for this league");
        return team;
    }

    private void log(FantasyTeam team, NflPlayer player, TransactionType type, String slot, String email) {
        RosterTransaction t = new RosterTransaction();
        t.setFantasyTeam(team);
        t.setNflPlayer(player);
        t.setUser(access.user(email));
        t.setType(type);
        t.setPositionSlot(slot);
        transactions.save(t);
    }

    static RosterEntryResponse toEntry(RosterPlayer rp) {
        NflPlayer p = rp.getNflPlayer();
        return new RosterEntryResponse(p.getId(), p.getName(), p.getPosition(), p.getNflTeam(), rp.getPositionSlot());
    }
}
