package com.fantasyzone.repository;

import com.fantasyzone.entity.RosterTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RosterTransactionRepository extends JpaRepository<RosterTransaction, Long> {
    List<RosterTransaction> findTop50ByFantasyTeamIdOrderByCreatedAtDesc(Long teamId);
}
