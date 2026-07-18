package com.yatayat.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.yatayat.backend.dto.TicketResponse;
import com.yatayat.backend.entity.Booking;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class TicketPdfService {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a");

    public byte[] generateTicketPdf(Booking booking) {
        try {
            String template = loadTemplate();
            String qrBase64 = generateQrBase64(booking.getQrCode());

            DecimalFormat df = new DecimalFormat("#,##0.00");

            String html = template
                    .replace("{{passengerName}}", safe(booking.getPassenger().getFullName()))
                    .replace("{{bookingId}}", String.valueOf(booking.getId()))
                    .replace("{{routeName}}", safe(booking.getRouteName()))
                    .replace("{{busNumber}}", safe(booking.getBusNumber()))
                    .replace("{{seatNumber}}", safe(booking.getSeatNumber()))
                    .replace("{{travelDate}}", safe(booking.getTravelDate()))
                    .replace("{{departureTime}}", safe(booking.getDepartureTime()))
                    .replace("{{fare}}", df.format(booking.getFare()))
                    .replace("{{bookingStatus}}", safe(booking.getBookingStatus()))
                    .replace("{{paymentStatus}}", safe(booking.getPaymentStatus()))
                    .replace("{{qrCode}}", safe(booking.getQrCode()))
                    .replace("{{qrBase64}}", qrBase64);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ticket PDF", e);
        }
    }

    public byte[] generatePassengerTripTicketPdf(TicketResponse ticket) {
        try {
            String qrBase64 = generateQrBase64(ticket.qrPayload());
            String html = passengerTicketHtml(ticket, qrBase64);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate passenger ticket PDF", exception);
        }
    }

    private String loadTemplate() throws Exception {
        try (var inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("templates/ticket-template.html")) {

            if (inputStream == null) {
                throw new RuntimeException("ticket-template.html not found");
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String generateQrBase64(String qrText) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix = qrCodeWriter.encode(
                qrText,
                BarcodeFormat.QR_CODE,
                250,
                250
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String html(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String safeAmount(java.math.BigDecimal value) {
        return new DecimalFormat("#,##0.00").format(value == null ? java.math.BigDecimal.ZERO : value);
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    private String passengerTicketHtml(TicketResponse ticket, String qrBase64) {
        String template = """
                <html>
                <head>
                  <style>
                    body { font-family: Arial, sans-serif; color:#0f172a; margin:0; background:#f8fafc; }
                    .page { max-width:760px; margin:24px auto; background:white; border-radius:22px; overflow:hidden; border:1px solid #e2e8f0; }
                    .header { background:#08264a; color:white; padding:30px; }
                    .brand { font-size:30px; font-weight:900; margin:0; }
                    .subtitle { color:#bfdbfe; margin:6px 0 0; font-size:14px; }
                    .body { padding:28px; }
                    .top { display:table; width:100%; }
                    .left { display:table-cell; vertical-align:top; width:58%; }
                    .right { display:table-cell; text-align:center; vertical-align:top; width:42%; }
                    .qr { width:220px; height:220px; border:1px solid #e2e8f0; border-radius:16px; padding:10px; }
                    .badge { display:inline-block; background:#dcfce7; color:#166534; border-radius:999px; padding:8px 14px; font-size:12px; font-weight:900; }
                    h1 { margin:10px 0 6px; font-size:28px; }
                    .route { font-size:22px; font-weight:900; margin:18px 0; color:#08264a; }
                    table { width:100%; border-collapse:collapse; margin-top:22px; font-size:13px; }
                    td { padding:11px 10px; border-bottom:1px solid #e2e8f0; }
                    td:first-child { color:#64748b; font-weight:700; width:35%; }
                    td:last-child { font-weight:800; text-align:right; }
                    .note { margin-top:24px; padding:16px; background:#eff6ff; border-radius:16px; color:#1e3a8a; font-weight:700; font-size:13px; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <div class="header">
                      <p class="brand">Yatayat</p>
                      <p class="subtitle">Electronic Bus Ticket</p>
                    </div>
                    <div class="body">
                      <div class="top">
                        <div class="left">
                          <span class="badge">{{ticketStatus}}</span>
                          <h1>Electronic Ticket</h1>
                          <p>Ticket No. <b>{{ticketNumber}}</b></p>
                          <p>Booking Ref. <b>{{bookingReference}}</b></p>
                          <div class="route">{{origin}} &#8594; {{destination}}</div>
                        </div>
                        <div class="right">
                          <img class="qr" src="data:image/png;base64,{{qrBase64}}" />
                          <p style="font-size:11px;color:#64748b;">Present this QR code before boarding.</p>
                        </div>
                      </div>
                      <table>
                        <tr><td>Passenger</td><td>{{passengerName}}</td></tr>
                        <tr><td>Travel Date</td><td>{{travelDate}}</td></tr>
                        <tr><td>Departure</td><td>{{departureAt}}</td></tr>
                        <tr><td>Operator</td><td>{{operatorName}}</td></tr>
                        <tr><td>Bus</td><td>{{busName}} ({{busNumber}})</td></tr>
                        <tr><td>Seats</td><td>{{seatNumbers}}</td></tr>
                        <tr><td>Boarding Point</td><td>{{boardingPoint}}</td></tr>
                        <tr><td>Drop-off Point</td><td>{{dropOffPoint}}</td></tr>
                        <tr><td>Fare Paid</td><td>NPR {{totalFare}}</td></tr>
                        <tr><td>Payment Method</td><td>{{paymentMethod}}</td></tr>
                        <tr><td>Payment Reference</td><td>{{paymentReference}}</td></tr>
                        <tr><td>Issued At</td><td>{{issuedAt}}</td></tr>
                        <tr><td>Valid Until</td><td>{{validUntil}}</td></tr>
                      </table>
                      <div class="note">Please arrive at least 15 minutes before departure. The backend remains the source of truth for ticket validity.</div>
                    </div>
                  </div>
                </body>
                </html>
                """;

        return template
                .replace("{{ticketStatus}}", html(ticket.ticketStatus()))
                .replace("{{ticketNumber}}", html(ticket.ticketNumber()))
                .replace("{{bookingReference}}", html(ticket.bookingReference()))
                .replace("{{origin}}", html(ticket.origin()))
                .replace("{{destination}}", html(ticket.destination()))
                .replace("{{qrBase64}}", qrBase64)
                .replace("{{passengerName}}", html(ticket.passengerName()))
                .replace("{{travelDate}}", format(ticket.travelDate()))
                .replace("{{departureAt}}", format(ticket.departureAt()))
                .replace("{{operatorName}}", html(ticket.operatorName()))
                .replace("{{busName}}", html(ticket.busName()))
                .replace("{{busNumber}}", html(ticket.busNumber()))
                .replace("{{seatNumbers}}", html(String.join(", ", ticket.seatNumbers())))
                .replace("{{boardingPoint}}", html(ticket.boardingPoint()))
                .replace("{{dropOffPoint}}", html(ticket.dropOffPoint()))
                .replace("{{totalFare}}", safeAmount(ticket.totalFare()))
                .replace("{{paymentMethod}}", html(ticket.paymentMethod()))
                .replace("{{paymentReference}}", html(ticket.paymentReference()))
                .replace("{{issuedAt}}", format(ticket.issuedAt()))
                .replace("{{validUntil}}", format(ticket.validUntil()));
    }
}
