package app.mealdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import app.mealdeck.dto.MealTemplateResponse;
import app.mealdeck.dto.PackingSlipDraft;
import app.mealdeck.dto.PackingSlipLineDraft;

/** Verifies conservative shipment-row classification. */
class PackingSlipMatcherTest {
    @Test
    void separatesKnownChangedAndUnknownRowsWithoutMutatingInventory() {
        MealTemplateService templates = mock(MealTemplateService.class);
        when(templates.find("3C")).thenReturn(Optional.of(template("3C", "Garlic & Herb Cheesy Breadsticks")));
        when(templates.find("D3H")).thenReturn(Optional.of(template("D3H", "Meatless Chorizo Tacos")));
        when(templates.find("F99")).thenReturn(Optional.empty());
        PackingSlipMatcher matcher = new PackingSlipMatcher(templates);

        var result = matcher.match(new PackingSlipDraft(
                "R2533747606",
                "7/8/2026",
                List.of(
                        new PackingSlipLineDraft("3C", 1, "Garlic & Herb Cheesy Breadsticks"),
                        new PackingSlipLineDraft("D3H", 2, "Meatless Chorizo Taco Bowl"),
                        new PackingSlipLineDraft("F99", null, "Sweet Thai Chili Crab Cakes"))));

        assertThat(result.lines()).extracting(line -> line.status())
                .containsExactly("KNOWN", "CHANGED", "UNKNOWN");
        assertThat(result.lines()).extracting(line -> line.quantity())
                .containsExactly(1, 2, 1);
    }

    private MealTemplateResponse template(String code, String name) {
        return new MealTemplateResponse(
                UUID.randomUUID(), "SUVIE", 1, code, null, null, name,
                null, null, null, null, null, null, null,
                null, null, Instant.now());
    }
}
