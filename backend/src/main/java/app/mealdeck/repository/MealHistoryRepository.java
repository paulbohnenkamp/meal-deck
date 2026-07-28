package app.mealdeck.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import app.mealdeck.entity.MealHistory;

public interface MealHistoryRepository extends JpaRepository<MealHistory, UUID> {
    List<MealHistory> findByConsumedAtAfterOrderByConsumedAtDesc(Instant since);
    List<MealHistory> findAllByOrderByConsumedAtDesc();
}
