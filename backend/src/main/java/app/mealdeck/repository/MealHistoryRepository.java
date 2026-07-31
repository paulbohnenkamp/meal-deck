package app.mealdeck.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import app.mealdeck.entity.MealHistory;

/** Provides persistence queries for immutable dinner-history snapshots. */
public interface MealHistoryRepository extends JpaRepository<MealHistory, UUID> {
    /**
     * @param since exclusive lower timestamp bound
     * @return recent entries newest first
     */
    List<MealHistory> findByConsumedAtAfterOrderByConsumedAtDesc(Instant since);

    /** @return every history entry newest first */
    List<MealHistory> findAllByOrderByConsumedAtDesc();
}
