package uk.gov.dwp.uc.pairtest;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.INVALID_ACCOUNT_ID;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.INVALID_TICKET_QUANTITY;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.INVALID_TICKET_REQUEST;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.MAX_TICKETS_EXCEEDED;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.NO_ADULT_TICKET;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.NO_TICKETS_REQUESTED;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.TOO_MANY_INFANTS;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode;

class TicketServiceImplTest {

    private record Payment(long accountId, int amount) {
    }

    private record Reservation(long accountId, int seats) {
    }

    private static final class RecordingPaymentService implements TicketPaymentService {
        private final List<Payment> payments = new ArrayList<>();

        @Override
        public void makePayment(long accountId, int totalAmountToPay) {
            payments.add(new Payment(accountId, totalAmountToPay));
        }
    }

    private static final class RecordingSeatReservationService implements SeatReservationService {
        private final List<Reservation> reservations = new ArrayList<>();

        @Override
        public void reserveSeat(long accountId, int totalSeatsToAllocate) {
            reservations.add(new Reservation(accountId, totalSeatsToAllocate));
        }
    }

    private static TicketTypeRequest request(Type type, int count) {
        return new TicketTypeRequest(type, count);
    }

    private static void assertPurchase(long accountId, int amount, int seats, TicketTypeRequest... requests) {
        RecordingPaymentService paymentService = new RecordingPaymentService();
        RecordingSeatReservationService seatService = new RecordingSeatReservationService();

        new TicketServiceImpl(paymentService, seatService).purchaseTickets(accountId, requests);

        assertEquals(List.of(new Payment(accountId, amount)), paymentService.payments);
        assertEquals(List.of(new Reservation(accountId, seats)), seatService.reservations);
    }

    private static void assertRejected(ErrorCode code, Long accountId, TicketTypeRequest... requests) {
        RecordingPaymentService payments = new RecordingPaymentService();
        RecordingSeatReservationService seats = new RecordingSeatReservationService();
        TicketServiceImpl service = new TicketServiceImpl(payments, seats);

        InvalidPurchaseException exception =
                assertThrows(InvalidPurchaseException.class, () -> service.purchaseTickets(accountId, requests));

        assertEquals(code, exception.getErrorCode());
        assertTrue(payments.payments.isEmpty(), "no payment should be taken");
        assertTrue(seats.reservations.isEmpty(), "no seats should be reserved");
    }

    @Nested
    @DisplayName("valid purchases")
    class ValidPurchases {

        @Test
        void singleAdultCosts25AndReservesOneSeat() {
            assertPurchase(17, 25, 1, request(ADULT, 1));
        }

        @Test
        void adultAndChildCost40AndReserveTwoSeats() {
            assertPurchase(5, 40, 2, request(ADULT, 1), request(CHILD, 1));
        }

        @Test
        void infantsAreFreeAndAreNotAllocatedASeat() {
            assertPurchase(1, 25, 1, request(ADULT, 1), request(INFANT, 1));
        }

        @Test
        void mixedPurchaseChargesAdultsAndChildrenOnly() {
            assertPurchase(27, 95, 5, request(ADULT, 2), request(CHILD, 3), request(INFANT, 2));
        }

        @Test
        void repeatedTicketTypesAreAccumulated() {
            assertPurchase(3, 80, 4, request(ADULT, 1), request(CHILD, 2), request(ADULT, 1));
        }

        @Test
        void exactly25TicketsAreAcceptedIncludingInfants() {
            assertPurchase(3, 325, 13, request(ADULT, 13), request(INFANT, 12));
        }

        @Test
        void largestAccountIdIsAccepted() {
            assertPurchase(Long.MAX_VALUE, 25, 1, request(ADULT, 1));
        }

        @Test
        void worksWithTheRealThirdPartyServicesByDefault() {
            assertDoesNotThrow(() -> new TicketServiceImpl().purchaseTickets(1L, request(ADULT, 1)));
        }
    }

    @Nested
    @DisplayName("account validation")
    class AccountValidation {

        @ParameterizedTest(name = "rejects account ID {0}")
        @NullSource
        @ValueSource(longs = {0, -1, Long.MIN_VALUE})
        void rejectsInvalidAccountIds(Long accountId) {
            assertRejected(INVALID_ACCOUNT_ID, accountId, request(ADULT, 1));
        }
    }

    @Nested
    @DisplayName("request validation")
    class RequestValidation {

        @Test
        void rejectsAPurchaseWithNoTicketRequests() {
            assertRejected(NO_TICKETS_REQUESTED, 1L);
        }

        @Test
        void rejectsANullRequestArray() {
            assertRejected(NO_TICKETS_REQUESTED, 1L, (TicketTypeRequest[]) null);
        }

        @Test
        void rejectsANullTicketRequest() {
            assertRejected(INVALID_TICKET_REQUEST, 1L, (TicketTypeRequest) null);
        }

        @Test
        void rejectsANullRequestAfterAValidOne() {
            assertRejected(INVALID_TICKET_REQUEST, 1L, request(ADULT, 2), null);
        }

        @ParameterizedTest(name = "rejects a quantity of {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        void rejectsNonPositiveQuantities(int count) {
            assertRejected(INVALID_TICKET_QUANTITY, 1L, request(ADULT, count));
            assertRejected(INVALID_TICKET_QUANTITY, 1L, request(ADULT, 1), request(CHILD, count));
        }
    }

    @Nested
    @DisplayName("business rules")
    class BusinessRules {

        @Test
        void rejectsMoreThan25TicketsOfASingleType() {
            assertRejected(MAX_TICKETS_EXCEEDED, 1L, request(ADULT, 26));
        }

        @Test
        void the25TicketLimitAppliesAcrossRepeatedTypes() {
            assertRejected(MAX_TICKETS_EXCEEDED, 1L, request(ADULT, 13), request(ADULT, 13));
        }

        @Test
        void the25TicketLimitIncludesChildrenAndInfants() {
            assertRejected(MAX_TICKETS_EXCEEDED, 1L, request(ADULT, 1), request(CHILD, 25));
            assertRejected(MAX_TICKETS_EXCEEDED, 1L, request(ADULT, 13), request(INFANT, 13));
        }

        @Test
        void hugeQuantitiesCannotOverflowPastTheLimit() {
            // With int arithmetic these would wrap round to a total of 1
            assertRejected(MAX_TICKETS_EXCEEDED, 1L,
                    request(ADULT, Integer.MAX_VALUE), request(ADULT, Integer.MAX_VALUE), request(ADULT, 3));
        }

        @Test
        void rejectsChildTicketsWithoutAnAdult() {
            assertRejected(NO_ADULT_TICKET, 1L, request(CHILD, 1));
        }

        @Test
        void rejectsInfantTicketsWithoutAnAdult() {
            assertRejected(NO_ADULT_TICKET, 1L, request(INFANT, 1));
            assertRejected(NO_ADULT_TICKET, 1L, request(CHILD, 2), request(INFANT, 1));
        }

        @Test
        void rejectsMoreInfantsThanAdults() {
            assertRejected(TOO_MANY_INFANTS, 1L, request(ADULT, 1), request(INFANT, 2));
            assertRejected(TOO_MANY_INFANTS, 1L, request(ADULT, 1), request(ADULT, 1), request(INFANT, 3));
        }
    }

}
