package com.yatayat.backend.trip;

import com.yatayat.backend.dto.TicketResponse;
import com.yatayat.backend.service.TicketPdfService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPdfServiceTests {

    @Test
    void passengerTripTicketPdfGeneratesWithCssPercentValues() {
        TicketPdfService service = new TicketPdfService();
        LocalDateTime departure = LocalDateTime.now().plusDays(2);
        TicketResponse ticket = new TicketResponse(
                "YT-TKT-20260718-ABC123",
                "YAT-20260718-ABC123",
                "VALID",
                "Passenger & A",
                "Kathmandu <Main>",
                "Pokhara & Lakeside",
                "Safe & Fast Travels",
                "Deluxe \"Express\"",
                "BA-1-KHA-1000",
                departure,
                departure,
                "Gate 2 & Counter A",
                "Pokhara <Bus Park>",
                List.of("1A", "1B"),
                new BigDecimal("1000.00"),
                "WALLET",
                "PAY-20260718-ABC12345",
                LocalDateTime.now(),
                LocalDateTime.now(),
                departure.plusHours(8),
                "{\"version\":1,\"ticketNumber\":\"YT-TKT-20260718-ABC123\",\"token\":\"test-token\"}"
        );

        byte[] pdf = service.generatePassengerTripTicketPdf(ticket);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, Math.min(pdf.length, 4))).isEqualTo("%PDF");
    }
}
