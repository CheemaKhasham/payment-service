package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

@Configuration
class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@RestController
@RequestMapping("/api/payments")
class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Value("${notification.service.url:http://notification-service:8080}")
    private String notificationServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> paymentRequest) {
        long startTime = System.currentTimeMillis();
        String orderId = (String) paymentRequest.get("orderId");
        String paymentId = UUID.randomUUID().toString();
        Double amount = ((Number) paymentRequest.getOrDefault("amount", 0.0)).doubleValue();

        logger.info("Processing payment {} for order: {}, amount: ${}", paymentId, orderId, amount);

        try {
            // Simulate payment processing (quick - no CPU bottleneck here)
            Thread.sleep(10); // Small delay to simulate gateway call

            // Call notification service
            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("orderId", orderId);
            notificationRequest.put("paymentId", paymentId);
            notificationRequest.put("type", "PAYMENT_SUCCESS");
            notificationRequest.put("message", "Payment of $" + String.format("%.2f", amount) + " processed successfully");

            ResponseEntity<Map> notificationResponse = restTemplate.postForEntity(
                notificationServiceUrl + "/api/notifications/send",
                notificationRequest,
                Map.class
            );

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Payment {} for order {} completed in {}ms", paymentId, orderId, duration);

            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", paymentId);
            response.put("orderId", orderId);
            response.put("status", "APPROVED");
            response.put("amount", amount);
            response.put("processingTimeMs", duration);
            response.put("notificationStatus", notificationResponse.getBody());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Payment {} failed for order {}: {}", paymentId, orderId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("paymentId", paymentId);
            errorResponse.put("orderId", orderId);
            errorResponse.put("status", "FAILED");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String paymentId) {
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", paymentId);
        response.put("status", "APPROVED");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "payment-service");
        return ResponseEntity.ok(health);
    }
}
