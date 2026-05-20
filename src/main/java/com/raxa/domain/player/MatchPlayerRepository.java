package com.raxa.domain.player;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, UUID> {

    int countByMatchId(UUID matchId);
}
