package com.fantasyzone.repository;

import com.fantasyzone.entity.LeagueMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, Long> {
    boolean existsByUserIdAndLeagueId(Long userId, Long leagueId);
    Optional<LeagueMember> findByUserIdAndLeagueId(Long userId, Long leagueId);
    List<LeagueMember> findByLeagueId(Long leagueId);
    long countByLeagueId(Long leagueId);
    List<LeagueMember> findByUserId(Long userId);
}
