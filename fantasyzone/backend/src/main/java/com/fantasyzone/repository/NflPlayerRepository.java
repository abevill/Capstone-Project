package com.fantasyzone.repository;

import com.fantasyzone.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface NflPlayerRepository extends JpaRepository<NflPlayer, Long> {
    Page<NflPlayer> findByPosition(String position, Pageable pageable);
    Page<NflPlayer> findByNflTeam(String nflTeam, Pageable pageable);
    Page<NflPlayer> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<NflPlayer> findByExternalId(String externalId);
}
