package tn.esprit.tpfoyer.Controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import tn.esprit.tpfoyer.Dto.EnrollmentDTO;
import tn.esprit.tpfoyer.Dto.CheckoutSessionRequest;
import tn.esprit.tpfoyer.Dto.CheckoutSessionResponse;
import tn.esprit.tpfoyer.Entities.Course;
import tn.esprit.tpfoyer.Entities.LearningPath;
import tn.esprit.tpfoyer.Repositories.CourseRepository;
import tn.esprit.tpfoyer.Repositories.EnrollmentRepository;
import tn.esprit.tpfoyer.Repositories.LearningPathRepository;
import tn.esprit.tpfoyer.Services.IEnrollmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final EnrollmentRepository enrollmentRepository;
    private final IEnrollmentService enrollmentService;
    private final LearningPathRepository learningPathRepository;
    private final CourseRepository courseRepository;

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

    @PostMapping("/confirm-checkout-session")
    public ResponseEntity<?> confirmCheckoutSession(@RequestBody Map<String, String> request) throws Exception {
        String sessionId = request.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId is required"));
        }

        Session session = Session.retrieve(sessionId);
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Checkout session is not paid yet"));
        }

        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("studentId")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing studentId in Stripe metadata"));
        }

        Long studentId = Long.parseLong(metadata.get("studentId"));
        String paymentIntentId = session.getPaymentIntent() != null
                ? session.getPaymentIntent()
                : session.getId();

        List<EnrollmentDTO> enrollments = confirmPaidEnrollments(metadata, studentId, paymentIntentId);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "paymentIntentId", paymentIntentId,
                "enrollments", enrollments
        ));
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
            String rawJson = event.toJson();
            JsonObject eventJson = JsonParser.parseString(rawJson).getAsJsonObject();
            JsonObject sessionObj = eventJson
                .getAsJsonObject("data")
                .getAsJsonObject("object");

            Map<String, String> metadata = new HashMap<>();
            if (sessionObj.has("metadata") && !sessionObj.get("metadata").isJsonNull()) {
                sessionObj.getAsJsonObject("metadata").entrySet()
                    .forEach(e -> metadata.put(e.getKey(), e.getValue().getAsString()));
            }

            String paymentIntentId = sessionObj.has("payment_intent")
                && !sessionObj.get("payment_intent").isJsonNull()
                ? sessionObj.get("payment_intent").getAsString()
                : "unknown";

            Long studentId = metadata.containsKey("studentId")
                ? Long.parseLong(metadata.get("studentId"))
                : null;

            if (studentId == null) {
                log.warn("[Webhook] No studentId in metadata, skipping");
                return ResponseEntity.ok("skipped");
            }

            if (metadata.containsKey("pathId")) {
                // Learning path payment — enroll in all paid courses of the path
                Long pathId = Long.parseLong(metadata.get("pathId"));
                log.info("[Webhook] Path payment completed: pathId={} studentId={}", pathId, studentId);

                LearningPath path = learningPathRepository.findById(pathId).orElse(null);
                if (path != null && path.getCourseIds() != null) {
                    String[] courseIdArr = path.getCourseIds().split(",");
                    for (String cidStr : courseIdArr) {
                        try {
                            Long courseId = Long.parseLong(cidStr.trim());
                            Course course = courseRepository.findById(courseId).orElse(null);
                            if (course != null && course.getPrice() != null
                                    && course.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                                boolean alreadyEnrolled = enrollmentRepository
                                    .existsByStudentIdAndCourseId(studentId, courseId);
                                if (!alreadyEnrolled) {
                                    enrollmentService.enrollStudentWithPayment(
                                        studentId, courseId, paymentIntentId);
                                    log.info("[Webhook] Enrolled student {} in paid course {} via path {}",
                                        studentId, courseId, pathId);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("[Webhook] Failed to enroll in course {}: {}", cidStr, e.getMessage());
                        }
                    }
                }
            } else if (metadata.containsKey("courseId")) {
                // Single course payment
                Long courseId = Long.parseLong(metadata.get("courseId"));
                log.info("[Webhook] Single course payment: courseId={} studentId={}", courseId, studentId);
                enrollmentService.enrollStudentWithPayment(studentId, courseId, paymentIntentId);
            } else {
                log.warn("[Webhook] checkout.session.completed has no courseId or pathId in metadata");
            }
        }

        return ResponseEntity.ok("Received");
    }

    private List<EnrollmentDTO> confirmPaidEnrollments(
            Map<String, String> metadata,
            Long studentId,
            String paymentIntentId) {
        List<EnrollmentDTO> enrollments = new ArrayList<>();

        if (metadata.containsKey("pathId")) {
            Long pathId = Long.parseLong(metadata.get("pathId"));
            LearningPath path = learningPathRepository.findById(pathId).orElse(null);
            if (path != null && path.getCourseIds() != null) {
                String[] courseIdArr = path.getCourseIds().split(",");
                for (String cidStr : courseIdArr) {
                    try {
                        Long courseId = Long.parseLong(cidStr.trim());
                        Course course = courseRepository.findById(courseId).orElse(null);
                        if (course != null && course.getPrice() != null
                                && course.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                            enrollments.add(enrollmentService.enrollStudentWithPayment(
                                    studentId, courseId, paymentIntentId));
                        }
                    } catch (Exception e) {
                        log.warn("[Stripe confirm] Failed to enroll in course {}: {}", cidStr, e.getMessage());
                    }
                }
            }
            return enrollments;
        }

        if (metadata.containsKey("courseId")) {
            Long courseId = Long.parseLong(metadata.get("courseId"));
            enrollments.add(enrollmentService.enrollStudentWithPayment(studentId, courseId, paymentIntentId));
        }

        return enrollments;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of("publishableKey", stripePublishableKey));
    }
}
