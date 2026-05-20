package com.raxa.domain.match;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    @Query("""
            SELECT DISTINCT m FROM Match m
            LEFT JOIN MatchPlayer mp ON mp.match.id = m.id
            WHERE m.creator.id = :userId OR mp.user.id = :userId
            ORDER BY m.scheduledAt ASC
            """)
    List<Match> findAllByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Match m WHERE m.id = :matchId")
    Optional<Match> findByIdForUpdate(@Param("matchId") UUID matchId);
}
