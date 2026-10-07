package com.fantasyzone.repository;

import com.fantasyzone.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface LeagueRepository extends JpaRepository<League, Long> {
    Optional<League> findByJoinCode(String joinCode);
}
