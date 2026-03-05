package com.ybrainy.backend.service;

import com.ybrainy.backend.dto.cart.CartItemResponseDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutEmailService {

    private static final String TEMPLATE_PATH = "templates/ecom-invoice-email.html";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderAddress;

    @Value("${app.checkout.courses-url:http://localhost:4200/courses}")
    private String coursesUrl;

    public void sendCheckoutReceipt(String recipientEmail, CartResponseDTO cart) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Skipping checkout email: recipient email is empty.");
            return;
        }

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Skipping checkout email: checkout cart is empty for recipient {}", recipientEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(recipientEmail);
            helper.setSubject("YBrainy Invoice #" + cart.getId());
            if (senderAddress != null && !senderAddress.isBlank()) {
                helper.setFrom(senderAddress);
            }
            helper.setText(buildInvoiceHtml(recipientEmail, cart), true);
            mailSender.send(message);
            log.info("Checkout receipt sent successfully to {}", recipientEmail);
        } catch (Exception ex) {
            log.warn("Checkout succeeded but failed to send receipt email to {}: {}", recipientEmail, ex.getMessage());
        }
    }

    private String buildInvoiceHtml(String recipientEmail, CartResponseDTO cart) throws IOException {
        String template = loadInvoiceTemplate();
        double subtotal = computeSubtotal(cart);
        double total = cart.getTotalAmount() == null ? subtotal : cart.getTotalAmount();

        Map<String, String> values = Map.of(
                "{{ORDER_DATE}}", LocalDate.now().format(DATE_FORMATTER),
                "{{ORDER_ID}}", String.valueOf(cart.getId()),
                "{{RECIPIENT_EMAIL}}", escapeHtml(recipientEmail),
                "{{ITEM_ROWS}}", buildItemRows(cart),
                "{{SUBTOTAL}}", formatAmount(subtotal),
                "{{TOTAL}}", formatAmount(total),
                "{{COURSES_URL}}", escapeHtml(coursesUrl),
                "{{QR_IMAGE_URL}}", buildQrImageUrl()
        );

        String html = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }
        return html;
    }

    private String loadInvoiceTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String buildItemRows(CartResponseDTO cart) {
        StringBuilder rows = new StringBuilder();
        int index = 1;

        for (CartItemResponseDTO item : cart.getItems()) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            double unitPrice = item.getPriceAtPurchase() == null ? 0.0 : item.getPriceAtPurchase();
            double lineTotal = item.getSubtotal() == null ? (unitPrice * quantity) : item.getSubtotal();

            rows.append("<tr>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e5e7eb;color:#111827;\">")
                    .append(index++)
                    .append("</td>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e5e7eb;color:#111827;font-weight:600;\">")
                    .append(escapeHtml(item.getPackTitle() == null ? "Learning Pack" : item.getPackTitle()))
                    .append("</td>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e5e7eb;color:#4b5563;\">E-Learning Pack</td>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e5e7eb;text-align:right;color:#111827;\">$")
                    .append(formatAmount(unitPrice))
                    .append("</td>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e5e7eb;text-align:center;color:#111827;\">")
                    .append(quantity)
                    .append("</td>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e5e7eb;text-align:right;color:#111827;font-weight:600;\">$")
                    .append(formatAmount(lineTotal))
                    .append("</td>")
                    .append("</tr>");
        }

        return rows.toString();
    }

    private String buildQrImageUrl() {
        String encoded = URLEncoder.encode(coursesUrl, StandardCharsets.UTF_8);
        return "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + encoded;
    }

    private double computeSubtotal(CartResponseDTO cart) {
        return cart.getItems().stream()
                .mapToDouble(item -> {
                    if (item.getSubtotal() != null) {
                        return item.getSubtotal();
                    }
                    double unit = item.getPriceAtPurchase() == null ? 0.0 : item.getPriceAtPurchase();
                    int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                    return unit * qty;
                })
                .sum();
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatAmount(Double amount) {
        double value = amount == null ? 0.0 : amount;
        return String.format(Locale.US, "%.2f", value);
    }
}
