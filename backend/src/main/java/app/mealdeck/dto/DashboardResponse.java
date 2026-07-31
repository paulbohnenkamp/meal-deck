package app.mealdeck.dto;

/**
 * Aggregate inventory and random-draw eligibility counts.
 *
 * @param mealTypes number of distinct normalized meal names
 * @param totalBoxes total prepared-meal boxes in inventory
 * @param eligibleMealTypes meal names eligible for a strict draw
 * @param avoidDays recent-history window used for eligibility
 */
public record DashboardResponse(long mealTypes, int totalBoxes, long eligibleMealTypes, int avoidDays) {}
