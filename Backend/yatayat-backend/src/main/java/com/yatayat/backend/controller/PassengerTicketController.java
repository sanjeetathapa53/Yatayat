package com.yatayat.backend.controller;

import com.yatayat.backend.dto.TicketResponse;
import com.yatayat.backend.service.PassengerTicketService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/passenger")
public class PassengerTicketController {
    private final PassengerTicketService ticketService;

    public PassengerTicketController(PassengerTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/bookings/{bookingReference}/ticket")
    public TicketResponse getByBookingReference(Authentication authentication,
                                                @PathVariable String bookingReference) {
        return ticketService.getByBookingReference(authentication.getName(), bookingReference);
    }

    @GetMapping("/tickets/{ticketNumber}")
    public TicketResponse getByTicketNumber(Authentication authentication,
                                            @PathVariable String ticketNumber) {
        return ticketService.getByTicketNumber(authentication.getName(), ticketNumber);
    }

    @GetMapping("/tickets/{ticketNumber}/pdf")
    public ResponseEntity<byte[]> pdf(Authentication authentication,
                                      @PathVariable String ticketNumber) {
        byte[] pdf = ticketService.pdf(authentication.getName(), ticketNumber);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Yatayat-Ticket-" + ticketNumber + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/tickets/{ticketNumber}/email")
    public Map<String, Object> email(Authentication authentication,
                                     @PathVariable String ticketNumber) {
        ticketService.sendEmail(authentication.getName(), ticketNumber);
        return Map.of("success", true, "message", "E-ticket sent to your registered email.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false, "message", exception.getReason() == null
                        ? "Request could not be completed" : exception.getReason()));
    }
}
