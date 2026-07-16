package com.yatayat.backend.controller;

import com.yatayat.backend.dto.BookingRequest;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.EmailService;
import com.yatayat.backend.service.TicketPdfService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.yatayat.backend.service.AuthenticatedUserService;

import java.util.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TicketPdfService ticketPdfService;
    private final AuthenticatedUserService authenticatedUserService;

    public BookingController(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            TicketPdfService ticketPdfService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.ticketPdfService = ticketPdfService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/create")
    @Transactional
    public Map<String, Object> createBooking(
            @RequestBody BookingRequest request,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        User user = authenticatedUserService.requireOwnedUser(
                authentication,
                request.getUserId()
        );

        boolean seatAlreadyBooked =
                bookingRepository.existsByBusNumberAndTravelDateAndSeatNumberAndBookingStatusNot(
                        request.getBusNumber(),
                        request.getTravelDate(),
                        request.getSeatNumber(),
                        "CANCELLED"
                );

        if (seatAlreadyBooked) {
            response.put("success", false);
            response.put("message", "Seat already booked");
            return response;
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        if (wallet.getWalletPin() == null || wallet.getWalletPin().isBlank()) {
            response.put("success", false);
            response.put("message", "Wallet PIN not set");
            return response;
        }

        if (request.getWalletPin() == null || request.getWalletPin().isBlank()) {
            response.put("success", false);
            response.put("message", "Wallet PIN required");
            return response;
        }

        if (!passwordEncoder.matches(request.getWalletPin(), wallet.getWalletPin())) {
            response.put("success", false);
            response.put("message", "Incorrect wallet PIN");
            return response;
        }

        if (wallet.getBalance() < request.getFare()) {
            response.put("success", false);
            response.put("message", "Insufficient wallet balance");
            return response;
        }

        wallet.setBalance(wallet.getBalance() - request.getFare());
        walletRepository.save(wallet);

        transactionRepository.save(new WalletTransaction(
                wallet,
                "TICKET_PAYMENT",
                request.getFare(),
                "SUCCESS",
                "WALLET"
        ));

        Booking booking = new Booking(
                user,
                request.getRouteName(),
                request.getBusNumber(),
                request.getSeatNumber(),
                request.getTravelDate(),
                request.getDepartureTime(),
                request.getFare(),
                "YATAYAT-" + UUID.randomUUID()
        );

        bookingRepository.save(booking);

        try {
            emailService.sendTicketEmail(user.getEmail(), user.getFullName(), booking);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        response.put("success", true);
        response.put("message", "Booking successful");
        response.put("booking", cleanBooking(booking));
        return response;
    }

    @PutMapping("/{bookingId}/cancel")
    @Transactional
    public Map<String, Object> cancelBooking(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        User authenticatedUser = authenticatedUserService.requireUser(authentication);
        Booking booking = bookingRepository
                .findByIdAndPassenger(bookingId, authenticatedUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if ("CANCELLED".equals(booking.getBookingStatus())) {
            response.put("success", false);
            response.put("message", "Booking already cancelled");
            return response;
        }

        User user = booking.getPassenger();

        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));

        wallet.setBalance(wallet.getBalance() + booking.getFare());
        walletRepository.save(wallet);

        transactionRepository.save(new WalletTransaction(
                wallet,
                "REFUND",
                booking.getFare(),
                "SUCCESS",
                "WALLET"
        ));

        booking.setBookingStatus("CANCELLED");
        booking.setPaymentStatus("REFUNDED");
        bookingRepository.save(booking);

        try {
            emailService.sendCancellationEmail(user.getEmail(), user.getFullName(), booking);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        response.put("success", true);
        response.put("message", "Booking cancelled and refunded");
        response.put("booking", cleanBooking(booking));
        return response;
    }

    @GetMapping("/{bookingId}/ticket-pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        User authenticatedUser = authenticatedUserService.requireUser(authentication);
        Booking booking = bookingRepository
                .findByIdAndPassenger(bookingId, authenticatedUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        byte[] pdf = ticketPdfService.generateTicketPdf(booking);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Ticket_YT-" + booking.getId() + ".pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/user/{userId}")
    public Object getUserBookings(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        User user = authenticatedUserService.requireOwnedUser(authentication, userId);

        return bookingRepository.findByPassengerOrderByCreatedAtDesc(user)
                .stream()
                .map(this::cleanBooking)
                .toList();
    }
    @PostMapping("/validate-qr")
    public Map<String, Object> validateQr(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String qrCode = request.get("qrCode");

        if (qrCode == null || qrCode.isBlank()) {
            response.put("success", false);
            response.put("status", "INVALID");
            response.put("message", "QR code is required");
            return response;
        }

        Booking booking = bookingRepository.findByQrCode(qrCode).orElse(null);

        if (booking == null) {
            response.put("success", false);
            response.put("status", "INVALID");
            response.put("message", "Invalid ticket");
            return response;
        }

        if ("CANCELLED".equals(booking.getBookingStatus())) {
            response.put("success", false);
            response.put("status", "CANCELLED");
            response.put("message", "This ticket has been cancelled");
            response.put("booking", cleanBooking(booking));
            return response;
        }

        if ("USED".equals(booking.getBookingStatus())) {
            response.put("success", false);
            response.put("status", "USED");
            response.put("message", "This ticket has already been used");
            response.put("booking", cleanBooking(booking));
            return response;
        }

        response.put("success", true);
        response.put("status", "VALID");
        response.put("message", "Valid ticket");
        response.put("booking", cleanBooking(booking));

        return response;
    }
    @PostMapping("/mark-used")
    @Transactional
    public Map<String, Object> markTicketAsUsed(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String qrCode = request.get("qrCode");

        if (qrCode == null || qrCode.isBlank()) {
            response.put("success", false);
            response.put("status", "INVALID");
            response.put("message", "QR code is required");
            return response;
        }

        Booking booking = bookingRepository.findByQrCode(qrCode).orElse(null);

        if (booking == null) {
            response.put("success", false);
            response.put("status", "INVALID");
            response.put("message", "Invalid ticket");
            return response;
        }

        if ("CANCELLED".equals(booking.getBookingStatus())) {
            response.put("success", false);
            response.put("status", "CANCELLED");
            response.put("message", "Cancelled ticket cannot be used");
            response.put("booking", cleanBooking(booking));
            return response;
        }

        if ("USED".equals(booking.getBookingStatus())) {
            response.put("success", false);
            response.put("status", "USED");
            response.put("message", "Ticket already used");
            response.put("booking", cleanBooking(booking));
            return response;
        }

        booking.setBookingStatus("USED");
        bookingRepository.save(booking);

        response.put("success", true);
        response.put("status", "USED");
        response.put("message", "Boarding confirmed. Ticket marked as used.");
        response.put("booking", cleanBooking(booking));

        return response;
    }

    private Map<String, Object> cleanBooking(Booking booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("routeName", booking.getRouteName());
        map.put("busNumber", booking.getBusNumber());
        map.put("seatNumber", booking.getSeatNumber());
        map.put("travelDate", booking.getTravelDate());
        map.put("departureTime", booking.getDepartureTime());
        map.put("fare", booking.getFare());
        map.put("paymentStatus", booking.getPaymentStatus());
        map.put("bookingStatus", booking.getBookingStatus());
        map.put("qrCode", booking.getQrCode());
        map.put("createdAt", booking.getCreatedAt());
        return map;
    }
}
