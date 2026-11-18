package uk.gov.dwp.uc.pairtest;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.INVALID_ACCOUNT_ID;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.INVALID_TICKET_QUANTITY;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.INVALID_TICKET_REQUEST;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.MAX_TICKETS_EXCEEDED;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.NO_ADULT_TICKET;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.NO_TICKETS_REQUESTED;
import static uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.ErrorCode.TOO_MANY_INFANTS;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketRules;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private final TicketPaymentService paymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl() {
        this(new TicketPaymentServiceImpl(), new SeatReservationServiceImpl());
    }

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService seatReservationService) {
        this.paymentService = Objects.requireNonNull(paymentService, "paymentService must not be null");
        this.seatReservationService =
                Objects.requireNonNull(seatReservationService, "seatReservationService must not be null");
    }

    /**
     * Should only have private methods other than the one below.
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccountId(accountId);
        Map<Type, Long> quantities = validateAndCountTickets(ticketTypeRequests);
        validateBusinessRules(quantities);

        // Only charge and reserve once everything is valid
        paymentService.makePayment(accountId, calculateTotalCost(quantities));
        seatReservationService.reserveSeat(accountId, calculateSeatsToReserve(quantities));
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException(INVALID_ACCOUNT_ID, "Account ID must be a positive number.");
        }
    }

    // Counts are summed as longs so that very large quantities can't overflow and slip under the limit
    private Map<Type, Long> validateAndCountTickets(TicketTypeRequest[] ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException(NO_TICKETS_REQUESTED, "At least one ticket must be purchased.");
        }

        Map<Type, Long> quantities = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            quantities.put(type, 0L);
        }

        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request == null) {
                throw new InvalidPurchaseException(INVALID_TICKET_REQUEST, "Ticket requests must not be null.");
            }

            int quantity = request.getNoOfTickets();
            if (quantity <= 0) {
                throw new InvalidPurchaseException(INVALID_TICKET_QUANTITY, "Ticket quantities must be positive.");
            }

            quantities.merge(request.getTicketType(), (long) quantity, Long::sum);
        }

        return quantities;
    }

    private void validateBusinessRules(Map<Type, Long> quantities) {
        long totalTickets = quantities.values().stream().mapToLong(Long::longValue).sum();
        if (totalTickets > TicketRules.MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException(MAX_TICKETS_EXCEEDED,
                    "A purchase can include at most " + TicketRules.MAX_TICKETS_PER_PURCHASE + " tickets.");
        }

        long adults = quantities.get(Type.ADULT);
        if (adults == 0) {
            throw new InvalidPurchaseException(NO_ADULT_TICKET, "At least one adult ticket is required.");
        }

        // Each infant needs an adult's lap, so one adult cannot accompany two infants
        if (quantities.get(Type.INFANT) > adults) {
            throw new InvalidPurchaseException(TOO_MANY_INFANTS, "Each infant must be accompanied by an adult.");
        }
    }

    // Safe to narrow to int: the ticket limit has already been enforced
    private int calculateTotalCost(Map<Type, Long> quantities) {
        long total = 0;
        for (Map.Entry<Type, Long> entry : quantities.entrySet()) {
            total += entry.getValue() * TicketRules.priceOf(entry.getKey());
        }
        return Math.toIntExact(total);
    }

    private int calculateSeatsToReserve(Map<Type, Long> quantities) {
        long seats = 0;
        for (Map.Entry<Type, Long> entry : quantities.entrySet()) {
            if (TicketRules.requiresSeat(entry.getKey())) {
                seats += entry.getValue();
            }
        }
        return Math.toIntExact(seats);
    }

}
