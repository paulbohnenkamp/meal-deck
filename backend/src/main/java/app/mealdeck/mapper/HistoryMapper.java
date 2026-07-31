package app.mealdeck.mapper;

import org.springframework.stereotype.Component;

import app.mealdeck.dto.HistoryResponse;
import app.mealdeck.entity.MealHistory;

@Component
/** Maps persistent history snapshots to immutable API responses. */
public class HistoryMapper {
    /**
     * @param history persisted history snapshot
     * @return client-facing history representation
     */
    public HistoryResponse toResponse(MealHistory history) {
        return new HistoryResponse(
                history.getId(),
                history.getMealId(),
                history.getMealName(),
                history.getCarbsPerServingSnapshot(),
                history.getServingsSnapshot(),
                history.getConsumedAt());
    }
}
