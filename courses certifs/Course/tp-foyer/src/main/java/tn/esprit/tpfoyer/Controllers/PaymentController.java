package tn.esprit.tpfoyer.Controllers;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import tn.esprit.tpfoyer.Dto.CheckoutSessionRequest;
import tn.esprit.tpfoyer.Dto.CheckoutSessionResponse;
import tn.esprit.tpfoyer.Repositories.EnrollmentRepository;
import tn.esprit.tpfoyer.Services.IEnrollmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final EnrollmentRepository enrollmentRepository;
    private final IEnrollmentService enrollmentService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @RequestBody CheckoutSessionRequest request) throws Exception {

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(Math.round(request.getPrice() * 100))
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(request.getCourseTitle())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .setSuccessUrl(frontendUrl + "/courses/" + request.getCourseId()
                        + "?payment=success&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/courses/" + request.getCourseId())
                .putMetadata("courseId", request.getCourseId().toString())
                .putMetadata("studentId", request.getStudentId().toString())
                .build();

        Session session = Session.create(params);

        return ResponseEntity.ok(new CheckoutSessionResponse(session.getId(), session.getUrl()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Webhook error: " + e.getMessage());
        }

        if ("checkout.session.completed".equals(event.getType())) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            if (deserializer.getObject().isPresent()) {
                Session session = (Session) deserializer.getObject().get();
                Map<String, String> metadata = session.getMetadata();
                Long courseId  = Long.parseLong(metadata.get("courseId"));
                Long studentId = Long.parseLong(metadata.get("studentId"));
                String paymentIntentId = session.getPaymentIntent();

                if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
                    enrollmentService.enrollStudentWithPayment(studentId, courseId, paymentIntentId);
                }
            }
        }

        return ResponseEntity.ok("Received");
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of("publishableKey", stripePublishableKey));
    }
}
