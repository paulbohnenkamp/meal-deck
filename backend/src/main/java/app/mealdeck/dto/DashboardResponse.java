package app.mealdeck.dto;

/**
 * Aggregate inventory and random-draw eligibility counts.
 *
 * @param mealTypes number of distinct meal names with at least one box
 * @param totalBoxes total prepared-meal boxes in inventory
 * @param eligibleMealTypes meal names eligible for a strict draw
 * @param avoidDays recent-history window used for eligibility
 */
public record DashboardResponse(long mealTypes, int totalBoxes, long eligibleMealTypes, int avoidDays) {}
