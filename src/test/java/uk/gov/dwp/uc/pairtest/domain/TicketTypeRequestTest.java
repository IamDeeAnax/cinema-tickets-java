package uk.gov.dwp.uc.pairtest.domain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;

class TicketTypeRequestTest {

    @Test
    void storesTheTicketTypeAndCount() {
        TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 2);

        assertEquals(Type.ADULT, request.getTicketType());
        assertEquals(2, request.getNoOfTickets());
    }

    @Test
    void rejectsANullTicketType() {
        assertThrows(NullPointerException.class, () -> new TicketTypeRequest(null, 1));
    }

    @Test
    void isImmutable() {
        assertTrue(Modifier.isFinal(TicketTypeRequest.class.getModifiers()), "class should be final");
        for (Field field : TicketTypeRequest.class.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue; // e.g. fields added by coverage tools
            }
            assertTrue(Modifier.isFinal(field.getModifiers()), field.getName() + " should be final");
        }
    }

}
