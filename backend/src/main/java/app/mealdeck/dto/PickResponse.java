package app.mealdeck.dto;

public record PickResponse(MealResponse meal, HistoryResponse history, int avoidDays) {}
