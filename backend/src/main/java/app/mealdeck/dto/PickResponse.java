package app.mealdeck.dto;

/**
 * Result of consuming a selected meal.
 *
 * @param meal meal after its inventory decrement
 * @param history immutable history snapshot created for the dinner
 * @param avoidDays recent-history window associated with the selection
 */
public record PickResponse(MealResponse meal, HistoryResponse history, int avoidDays) {}
