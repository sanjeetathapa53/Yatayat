package com.yatayat.backend.controller;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.service.PassengerBookingService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passenger/bookings")
public class PassengerBookingController {
    private final PassengerBookingService bookingService;
    public PassengerBookingController(PassengerBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<PassengerBookingDetailsResponse> create(
            Authentication authentication, @RequestBody CreatePassengerBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.create(authentication.getName(), request));
    }
    @GetMapping
    public List<PassengerBookingSummaryResponse> list(Authentication authentication) {
        return bookingService.list(authentication.getName());
    }
    @GetMapping("/{bookingReference}")
    public PassengerBookingDetailsResponse details(Authentication authentication,
                                                    @PathVariable String bookingReference) {
        return bookingService.details(authentication.getName(), bookingReference);
    }
    @PostMapping("/{bookingReference}/cancel")
    public PassengerBookingDetailsResponse cancel(Authentication authentication,
                                                   @PathVariable String bookingReference) {
        return bookingService.cancel(authentication.getName(), bookingReference);
    }
    @PostMapping("/{bookingReference}/pay/wallet")
    public WalletBookingPaymentResponse payWithWallet(Authentication authentication,
                                                       @PathVariable String bookingReference,
                                                       @RequestBody(required = false) WalletPinRequest request) {
        return bookingService.payWithWallet(authentication.getName(), bookingReference,
                request == null ? null : request.getWalletPin());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false, "message", exception.getReason() == null
                        ? "Request could not be completed" : exception.getReason()));
    }
}
