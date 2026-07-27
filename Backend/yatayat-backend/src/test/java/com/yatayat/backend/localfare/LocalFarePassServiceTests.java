package com.yatayat.backend.localfare;

import com.yatayat.backend.dto.LocalFarePassPurchaseRequest;
import com.yatayat.backend.dto.LocalFareQuoteRequest;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.LocalFarePassQrTokenService;
import com.yatayat.backend.service.LocalFarePassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalFarePassServiceTests {
    @Mock UserRepository userRepository;
    @Mock RouteRepository routeRepository;
    @Mock RouteStopRepository routeStopRepository;
    @Mock WalletRepository walletRepository;
    @Mock WalletTransactionRepository transactionRepository;
    @Mock LocalFarePassRepository passRepository;
    @Mock PasswordEncoder passwordEncoder;

    LocalFarePassService service;
    User passenger;
    Route route;
    RouteStop boarding;
    RouteStop destination;
    Wallet wallet;

    @BeforeEach
    void setUp() {
        service = new LocalFarePassService(
                userRepository, routeRepository, routeStopRepository, walletRepository,
                transactionRepository, passRepository, passwordEncoder,
                new LocalFarePassQrTokenService(
                        "test-only-local-fare-pass-secret-at-least-32-characters"));
        passenger = new User("Passenger", "passenger@example.com", "9800000000", "encoded", "PASSENGER");
        passenger.setId(1L);
        route = route();
        boarding = stop(10L, 100L, "Stop A", 1, "0.00");
        destination = stop(11L, 200L, "Stop B", 3, "35.50");
        wallet = new Wallet(passenger);
        wallet.setId(5L);
        wallet.setWalletPin("encoded-pin");
        wallet.setBalance(100.0);
        lenient().when(userRepository.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        lenient().when(routeRepository.findByIdAndStatusAndTripType(20L, RouteStatus.ACTIVE, TripType.LOCAL))
                .thenReturn(Optional.of(route));
        lenient().when(routeStopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(20L))
                .thenReturn(List.of(boarding, destination));
    }

    @Test
    void quoteUsesAuthoritativeCumulativeFare() {
        var quote = service.quote(passenger.getEmail(), new LocalFareQuoteRequest(20L, 100L, 200L));
        assertThat(quote.fare()).isEqualByComparingTo("35.50");
        assertThat(quote.boardingStopOrder()).isEqualTo(1);
        assertThat(quote.destinationStopOrder()).isEqualTo(3);
    }

    @Test
    void destinationMustFollowBoarding() {
        assertThatThrownBy(() -> service.quote(
                passenger.getEmail(), new LocalFareQuoteRequest(20L, 200L, 100L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Destination must come after");
    }

    @Test
    void purchaseLocksWalletDebitsBackendFareAndCreatesOneTransactionAndPass() {
        when(walletRepository.findWithLockByUser(passenger)).thenReturn(Optional.of(wallet));
        when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(true);
        when(routeRepository.findById(20L)).thenReturn(Optional.of(route));
        when(passRepository.existsByPassNumber(any())).thenReturn(false);
        when(passRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.purchase(passenger.getEmail(),
                new LocalFarePassPurchaseRequest(20L, 100L, 200L, "1234"));

        assertThat(result.fare()).isEqualByComparingTo("35.50");
        assertThat(wallet.getBalance()).isEqualTo(64.5);
        ArgumentCaptor<WalletTransaction> transaction = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getType()).isEqualTo("LOCAL_FARE_PAYMENT");
        assertThat(transaction.getValue().getAmount()).isEqualTo(35.5);
        verify(passRepository, times(1)).saveAndFlush(any(LocalFarePass.class));
    }

    @Test
    void inactiveWalletIncorrectPinAndInsufficientBalanceAreRejected() {
        when(walletRepository.findWithLockByUser(passenger)).thenReturn(Optional.of(wallet));
        wallet.setWalletPin(null);
        assertThatThrownBy(() -> purchase("1234")).hasMessageContaining("not active");

        wallet.setWalletPin("encoded-pin");
        when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(false);
        assertThatThrownBy(() -> purchase("1234")).hasMessageContaining("Incorrect wallet PIN");

        when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(true);
        wallet.setBalance(10.0);
        assertThatThrownBy(() -> purchase("1234")).hasMessageContaining("Insufficient");
        verify(transactionRepository, never()).save(any());
        verify(passRepository, never()).saveAndFlush(any());
    }

    @Test
    void anotherPassengersPassReturnsNotFound() {
        when(passRepository.findByPassNumberAndPassenger("YT-LFP-OTHER", passenger))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.details(passenger.getEmail(), "YT-LFP-OTHER"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    private void purchase(String pin) {
        service.purchase(passenger.getEmail(),
                new LocalFarePassPurchaseRequest(20L, 100L, 200L, pin));
    }

    private Route route() {
        Route value = new Route();
        value.setId(20L);
        value.setCode("L-20");
        value.setName("Local Route");
        value.setOrigin("A");
        value.setDestination("B");
        value.setTripType(TripType.LOCAL);
        value.setStatus(RouteStatus.ACTIVE);
        return value;
    }

    private RouteStop stop(Long id, Long stopId, String name, int order, String fare) {
        BusStop busStop = new BusStop();
        busStop.setId(stopId);
        busStop.setName(name);
        busStop.setActive(true);
        RouteStop value = new RouteStop();
        value.setId(id);
        value.setRoute(route);
        value.setBusStop(busStop);
        value.setStopOrder(order);
        value.setCumulativeFare(new BigDecimal(fare));
        value.setActive(true);
        return value;
    }
}
