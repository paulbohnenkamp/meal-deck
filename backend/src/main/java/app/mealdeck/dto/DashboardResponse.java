package app.mealdeck.dto;

public record DashboardResponse(long mealTypes, int totalBoxes, long eligibleMealTypes, int avoidDays) {}
