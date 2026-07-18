package com.yatayat.backend.trip;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.PassengerBookingController;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.PassengerBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PassengerBookingController.class)
@Import({SecurityConfig.class, PassengerBookingService.class})
class PassengerBookingIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ScheduledTripRepository tripRepository;
    @MockitoBean private PassengerTripBookingRepository bookingRepository;
    @MockitoBean private BookingSeatRepository seatRepository;
    @MockitoBean private WalletRepository walletRepository;
    @MockitoBean private WalletTransactionRepository walletTransactionRepository;
    @MockitoBean private PaymentRepository paymentRepository;

    private User passengerA;
    private User passengerB;
    private ScheduledTrip trip;
    private PassengerTripBooking booking;

    @BeforeEach
    void setUp() {
        passengerA = new User("Passenger A", "a@example.com", "9800000001", "encoded", "PASSENGER");
        passengerA.setId(1L);
        passengerB = new User("Passenger B", "b@example.com", "9800000002", "encoded", "PASSENGER");
        passengerB.setId(2L);
        when(userRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(passengerA));
        when(userRepository.findByEmailIgnoreCase("b@example.com")).thenReturn(Optional.of(passengerB));
        trip = validTrip();
        booking = confirmedBooking(passengerA, "YAT-20260717-ABC123");
        when(bookingRepository.existsByBookingReference(anyString())).thenReturn(false);
        when(paymentRepository.existsByTransactionReference(anyString())).thenReturn(false);
        when(paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(any(), eq(PaymentStatus.SUCCESS)))
                .thenReturn(Optional.empty());
        when(bookingRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PassengerTripBooking saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(50L);
            if (saved.getBookedAt() == null) saved.setBookedAt(LocalDateTime.now());
            return saved;
        });
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void authenticatedPassengerCreatesBookingWithServerCalculatedFare() throws Exception {
        prepareHolds(2);
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                        .content(validRequest(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference").value(org.hamcrest.Matchers.matchesPattern("YAT-\\d{8}-[A-F0-9]{6}")))
                .andExpect(jsonPath("$.bookingStatus").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.seatNumbers[0]").value("1A"))
                .andExpect(jsonPath("$.farePerSeat").value(500.00))
                .andExpect(jsonPath("$.totalFare").value(1000.00))
                .andExpect(jsonPath("$.passengerPhone").value("******0001"))
                .andExpect(jsonPath("$.passenger").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());
        verify(userRepository).findByEmailIgnoreCase("a@example.com");
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void exactRemainingCapacitySucceeds() throws Exception {
        trip.setSeatCapacitySnapshot(4);
        prepareHolds(2);
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                .content(validRequest(2))).andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void localTripSeatBookingIsRejectedWithoutSaving() throws Exception {
        trip.getRoute().setTripType(TripType.LOCAL);
        prepareHolds(1);

        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                        .content(validRequest(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Local trips do not support seat reservations."));

        verify(seatRepository, never()).findByScheduledTripAndPassengerAndStatusOrderBySeatNumberAsc(any(), any(), any());
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void bookingWithoutMatchingHoldReturnsConflict() throws Exception {
        when(tripRepository.findPassengerVisibleByIdForUpdate(eq(10L), any(), anyList()))
                .thenReturn(Optional.of(trip));
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                        .content(validRequest(2))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An active seat hold is required for every selected seat."));
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void cancelledBookingsReleaseCapacityThroughConfirmedSeatAggregate() throws Exception {
        trip.setSeatCapacitySnapshot(2);
        prepareHolds(2);
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                .content(validRequest(2))).andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void tripLockIsAcquiredBeforeCapacityIsCalculated() throws Exception {
        prepareHolds(1);
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                .content(validRequest(1))).andExpect(status().isCreated());
        var order = inOrder(tripRepository, seatRepository, bookingRepository);
        order.verify(tripRepository).findPassengerVisibleByIdForUpdate(eq(10L), any(), anyList());
        order.verify(seatRepository).releaseExpired(eq(trip), any());
        order.verify(seatRepository).findByScheduledTripAndPassengerAndStatusOrderBySeatNumberAsc(
                trip, passengerA, BookingSeatStatus.HELD);
        order.verify(bookingRepository).saveAndFlush(any());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void nonPositiveSeatsAndInvalidPassengerFieldsAreRejected() throws Exception {
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                .content(validRequest(0))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                .content("{\"tripId\":10,\"passengerName\":\"X\",\"passengerPhone\":\"bad\",\"numberOfSeats\":1,\"seatNumbers\":[\"1A\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void missingOrInvisibleTripReturnsSafeNotFound() throws Exception {
        when(tripRepository.findPassengerVisibleByIdForUpdate(eq(99L), any(), anyList()))
                .thenReturn(Optional.empty());
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                .content(validRequestForTrip(99L, 1))).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void pastCancelledAndClosedTripsCannotBeBooked() throws Exception {
        for (TripStatus status : List.of(TripStatus.CANCELLED, TripStatus.IN_PROGRESS, TripStatus.COMPLETED)) {
            trip.setStatus(status);
            when(tripRepository.findPassengerVisibleByIdForUpdate(eq(10L), any(), anyList()))
                    .thenReturn(Optional.of(trip));
            mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                    .content(validRequest(1))).andExpect(status().isNotFound());
        }
        trip.setStatus(TripStatus.SCHEDULED); trip.setDepartureAt(LocalDateTime.now().minusHours(1));
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                .content(validRequest(1))).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void invalidRouteOperatorBusDriverAndDocumentsCannotBeBooked() throws Exception {
        assertInvalidResource(() -> trip.getRoute().setStatus(RouteStatus.INACTIVE));
        trip = validTrip(); assertInvalidResource(() -> trip.getOperator().setVerificationStatus(OperatorVerificationStatus.PENDING));
        trip = validTrip(); assertInvalidResource(() -> trip.getBus().setStatus(BusStatus.REJECTED));
        trip = validTrip(); assertInvalidResource(() -> trip.getBus().setPermitExpiryDate(trip.getDepartureAt().toLocalDate().minusDays(1)));
        trip = validTrip(); assertInvalidResource(() -> trip.getBus().setInsuranceExpiryDate(trip.getDepartureAt().toLocalDate().minusDays(1)));
        trip = validTrip(); assertInvalidResource(() -> trip.getDriver().setVerificationStatus(DriverVerificationStatus.PENDING));
        trip = validTrip(); assertInvalidResource(() -> trip.getDriver().setLicenseExpiryDate(trip.getDepartureAt().toLocalDate().minusDays(1)));
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void listReturnsOnlyAuthenticatedPassengersSafeBookings() throws Exception {
        when(bookingRepository.findByPassengerOrderByBookedAtDesc(passengerA)).thenReturn(List.of(booking));
        mockMvc.perform(get("/api/passenger/bookings")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingReference").value(booking.getBookingReference()))
                .andExpect(jsonPath("$[0].passengerName").doesNotExist())
                .andExpect(jsonPath("$[0].passenger").doesNotExist());
        verify(bookingRepository).findByPassengerOrderByBookedAtDesc(passengerA);
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void ownerCanViewSafeDetailsButOtherPassengerReceivesNotFound() throws Exception {
        when(bookingRepository.findByBookingReferenceAndPassenger(booking.getBookingReference(), passengerA))
                .thenReturn(Optional.of(booking));
        mockMvc.perform(get("/api/passenger/bookings/" + booking.getBookingReference()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passengerPhone").value("******0001"))
                .andExpect(jsonPath("$.scheduledTrip").doesNotExist());
        mockMvc.perform(get("/api/passenger/bookings/" + booking.getBookingReference())
                        .with(user("b@example.com").roles("PASSENGER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void passengerCanCancelOwnFutureBookingAndRepeatIsIdempotent() throws Exception {
        when(bookingRepository.findByBookingReferenceAndPassenger(booking.getBookingReference(), passengerA))
                .thenReturn(Optional.of(booking));
        mockMvc.perform(post("/api/passenger/bookings/" + booking.getBookingReference() + "/cancel"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bookingStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());
        mockMvc.perform(post("/api/passenger/bookings/" + booking.getBookingReference() + "/cancel"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void otherPassengerCannotCancelAndClosedBookingCannotBeCancelled() throws Exception {
        mockMvc.perform(post("/api/passenger/bookings/" + booking.getBookingReference() + "/cancel")
                        .with(user("b@example.com").roles("PASSENGER")))
                .andExpect(status().isNotFound());
        booking.getScheduledTrip().setDepartureAt(LocalDateTime.now().minusMinutes(1));
        when(bookingRepository.findByBookingReferenceAndPassenger(booking.getBookingReference(), passengerA))
                .thenReturn(Optional.of(booking));
        mockMvc.perform(post("/api/passenger/bookings/" + booking.getBookingReference() + "/cancel"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void pendingBookingCanBePaidWithWalletAndConfirmsSeats() throws Exception {
        PassengerTripBooking pending = pendingBooking();
        List<BookingSeat> seats = bookingSeats(pending, 2, LocalDateTime.now().plusMinutes(5));
        Wallet wallet = wallet(1500.0);
        when(bookingRepository.findOwnedByReferenceForPayment(pending.getBookingReference(), passengerA.getId()))
                .thenReturn(Optional.of(pending));
        when(seatRepository.findWithLockByBookingOrderBySeatNumberAsc(pending)).thenReturn(seats);
        when(walletRepository.findWithLockByUser(passengerA)).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/passenger/bookings/" + pending.getBookingReference() + "/pay/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentMethod").value("WALLET"))
                .andExpect(jsonPath("$.paidAmount").value(1000.00))
                .andExpect(jsonPath("$.walletBalance").value(500.00))
                .andExpect(jsonPath("$.seatNumbers[0]").value("1A"));

        verify(walletTransactionRepository).save(any(WalletTransaction.class));
        verify(seatRepository).saveAll(seats);
        org.assertj.core.api.Assertions.assertThat(wallet.getBalance()).isEqualTo(500.0);
        org.assertj.core.api.Assertions.assertThat(seats).allMatch(seat -> seat.getStatus() == BookingSeatStatus.CONFIRMED);
        org.assertj.core.api.Assertions.assertThat(pending.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void insufficientWalletBalanceDoesNotDeductOrConfirm() throws Exception {
        PassengerTripBooking pending = pendingBooking();
        List<BookingSeat> seats = bookingSeats(pending, 2, LocalDateTime.now().plusMinutes(5));
        Wallet wallet = wallet(100.0);
        when(bookingRepository.findOwnedByReferenceForPayment(pending.getBookingReference(), passengerA.getId()))
                .thenReturn(Optional.of(pending));
        when(seatRepository.findWithLockByBookingOrderBySeatNumberAsc(pending)).thenReturn(seats);
        when(walletRepository.findWithLockByUser(passengerA)).thenReturn(Optional.of(wallet));

        mockMvc.perform(post("/api/passenger/bookings/" + pending.getBookingReference() + "/pay/wallet"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Insufficient wallet balance."));

        verify(paymentRepository, never()).save(any());
        verify(walletTransactionRepository, never()).save(any());
        org.assertj.core.api.Assertions.assertThat(wallet.getBalance()).isEqualTo(100.0);
        org.assertj.core.api.Assertions.assertThat(pending.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void expiredSeatHoldIsReleasedAndPaymentRejected() throws Exception {
        PassengerTripBooking pending = pendingBooking();
        List<BookingSeat> seats = bookingSeats(pending, 2, LocalDateTime.now().minusSeconds(1));
        when(bookingRepository.findOwnedByReferenceForPayment(pending.getBookingReference(), passengerA.getId()))
                .thenReturn(Optional.of(pending));
        when(seatRepository.findWithLockByBookingOrderBySeatNumberAsc(pending)).thenReturn(seats);

        mockMvc.perform(post("/api/passenger/bookings/" + pending.getBookingReference() + "/pay/wallet"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Your seat hold has expired. Please select seats again."));

        verify(paymentRepository, never()).save(any());
        verify(walletRepository, never()).findWithLockByUser(any());
        org.assertj.core.api.Assertions.assertThat(pending.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        org.assertj.core.api.Assertions.assertThat(seats).allMatch(seat -> seat.getStatus() == BookingSeatStatus.RELEASED);
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void repeatedSuccessfulWalletPaymentDoesNotDeductAgain() throws Exception {
        PassengerTripBooking confirmed = confirmedBooking(passengerA, "YAT-20260717-PAID01");
        Payment payment = new Payment();
        payment.setBooking(confirmed); payment.setPassenger(passengerA); payment.setAmount(confirmed.getTotalFare());
        payment.setPaymentMethod(PaymentMethod.WALLET); payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now()); payment.setTransactionReference("PAY-20260718-ABC12345");
        when(bookingRepository.findOwnedByReferenceForPayment(confirmed.getBookingReference(), passengerA.getId()))
                .thenReturn(Optional.of(confirmed));
        when(paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(confirmed, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(payment));
        when(walletRepository.findByUser(passengerA)).thenReturn(Optional.of(wallet(700.0)));
        when(seatRepository.findByBookingOrderBySeatNumberAsc(confirmed))
                .thenReturn(bookingSeats(confirmed, 2, LocalDateTime.now()));

        mockMvc.perform(post("/api/passenger/bookings/" + confirmed.getBookingReference() + "/pay/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.walletBalance").value(700.00));

        verify(walletRepository, never()).findWithLockByUser(any());
        verify(walletTransactionRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void missingScheduledTripRelationshipFailsClearlyWithoutWalletDeduction() throws Exception {
        PassengerTripBooking pending = pendingBooking();
        pending.setScheduledTrip(null);
        when(bookingRepository.findOwnedByReferenceForPayment(pending.getBookingReference(), passengerA.getId()))
                .thenReturn(Optional.of(pending));

        mockMvc.perform(post("/api/passenger/bookings/" + pending.getBookingReference() + "/pay/wallet"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Booking is missing its scheduled trip."));

        verify(walletRepository, never()).findWithLockByUser(any());
        verify(walletTransactionRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PASSENGER")
    void anotherPassengerCannotPayBooking() throws Exception {
        mockMvc.perform(post("/api/passenger/bookings/" + booking.getBookingReference() + "/pay/wallet")
                        .with(user("b@example.com").roles("PASSENGER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/passenger/bookings")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/passenger/bookings/YAT-20260717-PENDING/pay/wallet"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void operatorDriverAndAdminRolesAreDeniedByService() throws Exception {
        for (String role : List.of("OPERATOR", "DRIVER", "ADMIN")) {
            User user = new User(role, "operator@example.com", "", "", role);
            when(userRepository.findByEmailIgnoreCase("operator@example.com")).thenReturn(Optional.of(user));
            mockMvc.perform(get("/api/passenger/bookings")).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/passenger/bookings/YAT-20260717-PENDING/pay/wallet"))
                    .andExpect(status().isForbidden());
        }
    }

    private void assertInvalidResource(Runnable mutation) throws Exception {
        mutation.run();
        when(tripRepository.findPassengerVisibleByIdForUpdate(eq(10L), any(), anyList()))
                .thenReturn(Optional.of(trip));
        mockMvc.perform(post("/api/passenger/bookings").contentType("application/json")
                .content(validRequest(1))).andExpect(status().isNotFound());
    }
    private void prepareHolds(int count) {
        when(tripRepository.findPassengerVisibleByIdForUpdate(eq(10L), any(), anyList()))
                .thenReturn(Optional.of(trip));
        List<BookingSeat> holds = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            BookingSeat seat = new BookingSeat(); String number = "1" + (char) ('A' + index);
            seat.setScheduledTrip(trip); seat.setPassenger(passengerA); seat.setSeatNumber(number);
            seat.setActiveSeatNumber(number); seat.setStatus(BookingSeatStatus.HELD);
            seat.setHeldAt(LocalDateTime.now()); seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
            holds.add(seat);
        }
        when(seatRepository.findByScheduledTripAndPassengerAndStatusOrderBySeatNumberAsc(
                trip, passengerA, BookingSeatStatus.HELD)).thenReturn(holds);
    }
    private String validRequest(int seats) { return validRequestForTrip(10L, seats); }
    private String validRequestForTrip(long tripId, int seats) {
        String selected = java.util.stream.IntStream.range(0, Math.max(0, seats))
                .mapToObj(index -> "\"1" + (char) ('A' + index) + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"tripId\":%d,\"passengerName\":\"Passenger A\",\"passengerPhone\":\"9800000001\",\"numberOfSeats\":%d,\"seatNumbers\":[%s],\"totalFare\":1}".formatted(tripId, seats, selected);
    }
    private PassengerTripBooking confirmedBooking(User owner, String reference) {
        PassengerTripBooking result = new PassengerTripBooking(); result.setId(50L);
        result.setBookingReference(reference); result.setPassenger(owner); result.setScheduledTrip(trip);
        result.setPassengerName(owner.getFullName()); result.setPassengerPhone(owner.getPhone());
        result.setNumberOfSeats(2); result.setFarePerSeat(trip.getFare());
        result.setTotalFare(trip.getFare().multiply(BigDecimal.valueOf(2)));
        result.setStatus(BookingStatus.CONFIRMED); result.setBookedAt(LocalDateTime.now()); return result;
    }
    private PassengerTripBooking pendingBooking() {
        PassengerTripBooking result = confirmedBooking(passengerA, "YAT-20260717-PENDING");
        result.setStatus(BookingStatus.PENDING_PAYMENT);
        return result;
    }
    private List<BookingSeat> bookingSeats(PassengerTripBooking owner, int count, LocalDateTime expiresAt) {
        List<BookingSeat> seats = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            BookingSeat seat = new BookingSeat();
            String number = "1" + (char) ('A' + index);
            seat.setBooking(owner); seat.setScheduledTrip(owner.getScheduledTrip());
            seat.setPassenger(owner.getPassenger()); seat.setSeatNumber(number);
            seat.setActiveSeatNumber(number); seat.setStatus(BookingSeatStatus.HELD);
            seat.setHeldAt(LocalDateTime.now()); seat.setHoldExpiresAt(expiresAt);
            seats.add(seat);
        }
        owner.setSeats(seats);
        return seats;
    }
    private Wallet wallet(double balance) {
        Wallet wallet = new Wallet(passengerA);
        wallet.setBalance(balance);
        return wallet;
    }
    private ScheduledTrip validTrip() {
        LocalDateTime departure = LocalDateTime.now().plusDays(3);
        com.yatayat.backend.entity.Route route = new com.yatayat.backend.entity.Route();
        route.setId(20L); route.setCode("KTM-PKR"); route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu"); route.setDestination("Pokhara"); route.setStatus(RouteStatus.ACTIVE);
        route.setTripType(TripType.OUT_OF_VALLEY);
        User operatorUser = new User("Operator", "op@example.com", "", "", "OPERATOR");
        TransportOperator operator = new TransportOperator(); operator.setId(30L); operator.setUser(operatorUser);
        operator.setName("Safe Travels"); operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);
        Bus bus = new Bus(); bus.setId(40L); bus.setBusNumber("BA-1-KHA-1000");
        bus.setStatus(BusStatus.APPROVED); bus.setSeatCapacity(40); bus.setPermitExpiryDate(departure.toLocalDate().plusYears(1));
        bus.setInsuranceExpiryDate(departure.toLocalDate().plusYears(1));
        DriverProfile driver = new DriverProfile(new User("Driver", "d@example.com", "", "", "DRIVER"));
        driver.setId(45L); driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseExpiryDate(departure.toLocalDate().plusYears(1));
        ScheduledTrip result = new ScheduledTrip(); result.setId(10L); result.setRoute(route); result.setOperator(operator);
        result.setBus(bus); result.setDriver(driver); result.setDepartureAt(departure);
        result.setEstimatedArrivalAt(departure.plusHours(6)); result.setFare(new BigDecimal("500.00"));
        result.setSeatCapacitySnapshot(40); result.setStatus(TripStatus.SCHEDULED); result.setBoardingNotes("Gate 2"); return result;
    }
}
