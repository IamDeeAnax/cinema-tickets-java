package uk.gov.dwp.uc.pairtest.domain;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;

/**
 * Single source of truth for ticket prices, seating and the purchase limit.
 *
 * The switches have no {@code default} branch, so adding a new ticket type will not compile
 * until its price and seating are defined here.
 */
public final class TicketRules {

    public static final int MAX_TICKETS_PER_PURCHASE = 25;

    private TicketRules() {
    }

    public static int priceOf(Type type) {
        return switch (type) {
            case ADULT -> 25;
            case CHILD -> 15;
            case INFANT -> 0;
        };
    }

    public static boolean requiresSeat(Type type) {
        return switch (type) {
            case ADULT, CHILD -> true;
            case INFANT -> false; // sits on an adult's lap
        };
    }

}
