package app.mealdeck.mapper;

import org.springframework.stereotype.Component;

import app.mealdeck.dto.HistoryResponse;
import app.mealdeck.entity.MealHistory;

@Component
public class HistoryMapper {
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
