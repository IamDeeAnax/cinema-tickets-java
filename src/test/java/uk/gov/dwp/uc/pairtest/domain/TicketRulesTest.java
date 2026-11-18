package uk.gov.dwp.uc.pairtest.domain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;

class TicketRulesTest {

    @Test
    void pricesMatchTheSpecification() {
        assertEquals(25, TicketRules.priceOf(Type.ADULT));
        assertEquals(15, TicketRules.priceOf(Type.CHILD));
        assertEquals(0, TicketRules.priceOf(Type.INFANT));
    }

    @Test
    void onlyInfantsSitWithoutASeat() {
        assertTrue(TicketRules.requiresSeat(Type.ADULT));
        assertTrue(TicketRules.requiresSeat(Type.CHILD));
        assertFalse(TicketRules.requiresSeat(Type.INFANT));
    }

    @Test
    void maximumOf25TicketsPerPurchase() {
        assertEquals(25, TicketRules.MAX_TICKETS_PER_PURCHASE);
    }

}
