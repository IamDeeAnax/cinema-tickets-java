# Cinema Tickets – Java

## Overview

My solution to the DWP Software Engineer cinema tickets coding exercise, written in Java.

`TicketServiceImpl` takes an account ID and one or more ticket requests. It rejects any request that breaks the business rules, then charges the correct amount through `TicketPaymentService` and reserves the correct number of seats through `SeatReservationService`.

The supplied `TicketService` interface and third-party code are unchanged.

## Business rules

Prices and seating are defined once in `src/main/java/uk/gov/dwp/uc/pairtest/domain/TicketRules.java`:

| Ticket | Price | Needs a seat |
|---|---|---|
| Infant | £0 | No (sits on an adult's lap) |
| Child | £15 | Yes |
| Adult | £25 | Yes |

For example, 2 adults, 3 children and 1 infant cost £95 and need 5 seats.

A purchase is rejected with an `InvalidPurchaseException`. Each rule has a fixed `ErrorCode`, so callers can handle errors without relying on the message text:

| Rejected when | `ErrorCode` |
|---|---|
| the account ID is null, zero or negative | `INVALID_ACCOUNT_ID` |
| no tickets are requested | `NO_TICKETS_REQUESTED` |
| a ticket request is `null` | `INVALID_TICKET_REQUEST` |
| any ticket quantity is zero or negative | `INVALID_TICKET_QUANTITY` |
| more than 25 tickets are requested in total (infants included) | `MAX_TICKETS_EXCEEDED` |
| there is no adult ticket | `NO_ADULT_TICKET` |
| there are more infants than adults (see Assumptions) | `TOO_MANY_INFANTS` |

Nothing is charged or reserved for a rejected purchase.

`TicketTypeRequest` itself throws a `NullPointerException` if it is given a `null` ticket type.

## Approach

- `purchaseTickets` is the only public method. It validates the request, then calls private methods to work out the cost and seats.
- All validation happens before the payment or seat reservation services are called.
- `TicketTypeRequest` is immutable: the class is `final` and its fields are `final`.
- `TicketRules` uses `switch` expressions with no `default`, so adding a new ticket type won't compile until its price and seating are defined.
- Ticket counts are added up as `long`s, so very large quantities can't overflow an `int` and slip under the 25-ticket limit.
- The payment and seat services can be passed into the `TicketServiceImpl` constructor for testing. The no-argument constructor uses the real ones.

## Assumptions

- **One infant per adult.** Infants sit on an adult's lap, so I've assumed each adult can only take one infant. The exercise doesn't state a ratio; if more are allowed, remove the check in `validateBusinessRules` and its test.
- Infants count towards the 25-ticket limit, as they are still tickets.
- The exercise states that payments and seat reservations always succeed, so the code doesn't handle either call failing (for example, refunding a payment if the seats can't be reserved).
- Every ticket request must be for at least one ticket. A request for 0 is rejected rather than ignored, as it doesn't buy anything.

## Testing

### Running the tests

You need Java 21 or later and Maven 3.6.3 or later.

1. Check your versions:
   ```sh
   java -version
   mvn -version
   ```
2. From the project folder, run:
   ```sh
   mvn test
   ```
3. You should see all 32 tests pass:
   ```
   Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
   ```

To run a single test class:
```sh
mvn test -Dtest=TicketServiceImplTest
```

### What the tests cover

**`TicketServiceImplTest` (26 tests)**
- *Valid purchases:* correct price and seats for adults, children and infants; repeated ticket types are added together; exactly 25 tickets is allowed; the largest account ID is accepted; works with the real third-party services
- *Account validation:* `null`, zero, negative and `Long.MIN_VALUE` account IDs
- *Request validation:* no requests, a `null` request array, `null` requests (alone or after a valid one), zero and negative quantities
- *Business rules:* the 25-ticket limit (including infants, repeated types and quantities large enough to overflow an `int`), no adult, more infants than adults

Every rejection test checks the `ErrorCode`, and that no payment or seat reservation was made.

**`TicketTypeRequestTest` (3 tests)**
- stores its type and count
- rejects a `null` ticket type
- is immutable (`final` class and fields)

**`TicketRulesTest` (3 tests)**
- prices, seating and the 25-ticket limit match the specification
