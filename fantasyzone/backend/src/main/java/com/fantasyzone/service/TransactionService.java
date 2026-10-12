package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.entity.FantasyTeam;
import com.fantasyzone.exception.NotFoundException;
import com.fantasyzone.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TransactionService {
    private final FantasyTeamRepository teams;
    private final RosterTransactionRepository transactions;
    private final LeagueAccess access;

    public TransactionService(FantasyTeamRepository teams, RosterTransactionRepository transactions, LeagueAccess access) {
        this.teams = teams; this.transactions = transactions; this.access = access;
    }

    /** The team's 50 most recent roster moves, newest first. Visible to league members. */
    @Transactional(readOnly = true)
    public List<DTO.TransactionResponse> history(Long teamId, String email) {
        FantasyTeam team = teams.findById(teamId).orElseThrow(() -> new NotFoundException("Team not found"));
        access.requireMember(team.getLeague().getId(), email);
        return transactions.findTop50ByFantasyTeamIdOrderByCreatedAtDesc(teamId).stream()
            .map(t -> new DTO.TransactionResponse(t.getId(), t.getType().name(), t.getNflPlayer().getId(),
                t.getNflPlayer().getName(), t.getNflPlayer().getPosition(), t.getPositionSlot(),
                t.getUser().getDisplayName(), t.getCreatedAt()))
            .toList();
    }
}
