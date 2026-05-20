package com.raxa.domain.player;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, UUID> {

    int countByMatchId(UUID matchId);

    boolean existsByMatchIdAndUserId(UUID matchId, UUID userId);

    List<MatchPlayer> findByMatchIdOrderByJoinedAtAsc(UUID matchId);

    void deleteByMatchIdAndUserId(UUID matchId, UUID userId);

    @Query("SELECT COUNT(mp) FROM MatchPlayer mp WHERE mp.match.id = :matchId")
    int countByMatchIdForUpdate(@Param("matchId") UUID matchId);
}
