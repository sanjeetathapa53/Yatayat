package com.yatayat.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.yatayat.backend.dto.TicketResponse;
import com.yatayat.backend.entity.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a");

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TicketPdfService ticketPdfService;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Yatayat Email Verification OTP");
        message.setText("Your Yatayat verification OTP is: " + otp);
        mailSender.send(message);
    }

    public void sendTicketEmail(String toEmail, String passengerName, Booking booking)
            throws MessagingException {

        byte[] qrImage = generateQrImage(booking.getQrCode());
        byte[] pdfTicket = ticketPdfService.generateTicketPdf(booking);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("🎫 Your Yatayat E-Ticket is Confirmed");

        String html = """
                <html>
                <body style="margin:0;background:#f3f6fa;font-family:Arial,sans-serif;color:#0f172a;">
                  <div style="max-width:680px;margin:30px auto;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.12);">
                    <div style="background:#08264a;color:#ffffff;padding:28px;text-align:center;">
                      <h1 style="margin:0;font-size:30px;">🚌 Yatayat Nepal</h1>
                      <p style="margin:8px 0 0;color:#cbd5e1;">Your journey starts here</p>
                    </div>

                    <div style="padding:28px;">
                      <h2 style="margin-top:0;">Hello %s 👋</h2>
                      <p>Your booking has been confirmed. Your PDF ticket is attached to this email.</p>

                      <div style="border:1px dashed #cbd5e1;border-radius:16px;padding:22px;margin-top:20px;">
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                          <tr><td style="padding:10px;color:#64748b;">Booking ID</td><td style="padding:10px;font-weight:700;">YT-%d</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Route</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Bus Number</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Seat</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Travel Date</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Departure</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Amount Paid</td><td style="padding:10px;font-weight:700;color:#047857;">NPR %.2f</td></tr>
                        </table>
                      </div>

                      <div style="margin-top:26px;text-align:center;background:#f8fafc;border-radius:16px;padding:24px;">
                        <h3 style="margin-top:0;">Boarding QR Code</h3>
                        <img src="cid:ticketQr" style="width:190px;height:190px;" />
                        <p style="font-size:12px;color:#64748b;word-break:break-all;">%s</p>
                      </div>

                      <p style="margin-top:24px;font-size:14px;">
                        Please arrive at least <b>15 minutes before departure</b>.
                      </p>

                      <p style="font-size:13px;color:#64748b;text-align:center;margin-top:28px;">
                        Thank you for choosing <b>Yatayat Nepal</b>. Have a safe journey ❤️
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                passengerName,
                booking.getId(),
                booking.getRouteName(),
                booking.getBusNumber(),
                booking.getSeatNumber(),
                booking.getTravelDate(),
                booking.getDepartureTime(),
                booking.getFare(),
                booking.getQrCode()
        );

        helper.setText(html, true);

        helper.addInline("ticketQr", new ByteArrayResource(qrImage), "image/png");

        helper.addAttachment(
                "Ticket_YT-" + booking.getId() + ".pdf",
                new ByteArrayResource(pdfTicket),
                "application/pdf"
        );

        helper.addAttachment(
                "Yatayat_QR_YT-" + booking.getId() + ".png",
                new ByteArrayResource(qrImage),
                "image/png"
        );

        mailSender.send(message);
    }
    public void sendCancellationEmail(String toEmail, String passengerName, Booking booking)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("❌ Your Yatayat Ticket Has Been Cancelled");

        String html = """
            <html>
            <body style="margin:0;background:#f3f6fa;font-family:Arial,sans-serif;color:#0f172a;">
              <div style="max-width:650px;margin:30px auto;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.12);">
                
                <div style="background:#7f1d1d;color:#ffffff;padding:28px;text-align:center;">
                  <h1 style="margin:0;font-size:28px;">Yatayat Ticket Cancelled</h1>
                  <p style="margin:8px 0 0;color:#fecaca;">Your booking has been cancelled successfully.</p>
                </div>

                <div style="padding:28px;">
                  <h2 style="margin-top:0;">Hello %s,</h2>

                  <p>Your ticket has been cancelled and the fare has been refunded to your Yatayat Wallet.</p>

                  <div style="border:1px dashed #fecaca;border-radius:16px;padding:22px;margin-top:20px;background:#fff1f2;">
                    <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                      <tr><td style="padding:10px;color:#64748b;">Booking ID</td><td style="padding:10px;font-weight:700;">YT-%d</td></tr>
                      <tr><td style="padding:10px;color:#64748b;">Route</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                      <tr><td style="padding:10px;color:#64748b;">Bus Number</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                      <tr><td style="padding:10px;color:#64748b;">Seat</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                      <tr><td style="padding:10px;color:#64748b;">Refund Amount</td><td style="padding:10px;font-weight:700;color:#047857;">NPR %.2f</td></tr>
                      <tr><td style="padding:10px;color:#64748b;">Status</td><td style="padding:10px;font-weight:700;color:#b91c1c;">CANCELLED / REFUNDED</td></tr>
                    </table>
                  </div>

                  <p style="margin-top:24px;font-size:14px;">
                    This QR ticket is no longer valid for boarding.
                  </p>

                  <p style="font-size:13px;color:#64748b;text-align:center;margin-top:28px;">
                    Thank you for using <b>Yatayat Nepal</b>.
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                passengerName,
                booking.getId(),
                booking.getRouteName(),
                booking.getBusNumber(),
                booking.getSeatNumber(),
                booking.getFare()
        );

        helper.setText(html, true);
        mailSender.send(message);
    }

    public void sendPassengerTripTicketEmail(String toEmail, TicketResponse ticket, byte[] pdfTicket)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("Your Yatayat E-Ticket — " + ticket.origin() + " to " + ticket.destination());

        String html = """
                <html>
                <body style="margin:0;background:#f3f6fa;font-family:Arial,sans-serif;color:#0f172a;">
                  <div style="max-width:680px;margin:30px auto;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.12);">
                    <div style="background:#08264a;color:#ffffff;padding:28px;text-align:center;">
                      <h1 style="margin:0;font-size:30px;">Yatayat</h1>
                      <p style="margin:8px 0 0;color:#cbd5e1;">Electronic Bus Ticket</p>
                    </div>
                    <div style="padding:28px;">
                      <h2 style="margin-top:0;">Payment confirmed</h2>
                      <p>Your Yatayat e-ticket is ready. The PDF ticket is attached to this email.</p>
                      <div style="border:1px dashed #cbd5e1;border-radius:16px;padding:22px;margin-top:20px;">
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                          <tr><td style="padding:10px;color:#64748b;">Ticket Number</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Booking Reference</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Route</td><td style="padding:10px;font-weight:700;">%s to %s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Travel Date</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Bus</td><td style="padding:10px;font-weight:700;">%s (%s)</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Seats</td><td style="padding:10px;font-weight:700;">%s</td></tr>
                          <tr><td style="padding:10px;color:#64748b;">Fare</td><td style="padding:10px;font-weight:700;color:#047857;">NPR %s</td></tr>
                        </table>
                      </div>
                      <p style="margin-top:24px;font-size:14px;">Present the QR code in your attached PDF ticket to the driver before boarding.</p>
                      <p style="font-size:13px;color:#64748b;text-align:center;margin-top:28px;">Thank you for choosing <b>Yatayat</b>. Have a safe journey.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                ticket.ticketNumber(), ticket.bookingReference(), ticket.origin(), ticket.destination(),
                DATE_TIME.format(ticket.departureAt()), ticket.busName(), ticket.busNumber(),
                String.join(", ", ticket.seatNumbers()), ticket.totalFare()
        );

        helper.setText(html, true);
        helper.addAttachment(
                "Yatayat-Ticket-" + ticket.ticketNumber() + ".pdf",
                new ByteArrayResource(pdfTicket),
                "application/pdf"
        );
        mailSender.send(message);
    }

    private byte[] generateQrImage(String qrText) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    qrText,
                    BarcodeFormat.QR_CODE,
                    250,
                    250
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }
}
