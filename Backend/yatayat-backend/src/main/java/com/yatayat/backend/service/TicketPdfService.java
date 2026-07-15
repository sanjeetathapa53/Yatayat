package com.yatayat.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.yatayat.backend.entity.Booking;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Base64;

@Service
public class TicketPdfService {

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
}