// package com.edpp.transaction.processor;

// import java.math.BigDecimal;

// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.*;
// import org.springframework.stereotype.Component;
// import org.springframework.web.client.RestTemplate;

// import java.math.BigDecimal;
// import java.util.HashMap;
// import java.util.Map;

// import com.edpp.transaction.dtoresponse.ProcessorResponse;
// import com.edpp.transaction.entity.Transaction;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.*;
// import org.springframework.stereotype.Component;
// import org.springframework.web.client.RestTemplate;

// import java.math.BigDecimal;
// import java.util.HashMap;
// import java.util.Map;

// @Component
// @Slf4j
// public class PaystackProcessor implements PaymentProcessor {

//     @Value("${paystack.secret-key:}")
//     private String secretKey;

//     private final RestTemplate restTemplate = new RestTemplate();
//     private final ObjectMapper objectMapper = new ObjectMapper();
//     private static final String BASE_URL = "https://api.paystack.co";

//     @Override
//     public ProcessorResponse processPayment(Transaction transaction) {
//         log.info("Processing payment via Paystack: {}", transaction.getTransactionReference());

//         // Check if Paystack is configured
//         if (secretKey == null || secretKey.isEmpty()) {
//             log.warn("Paystack not configured, simulating successful payment");
//             return simulatePaystackPayment(transaction);
//         }

//         try {
//             HttpHeaders headers = new HttpHeaders();
//             headers.setBearerAuth(secretKey);
//             headers.setContentType(MediaType.APPLICATION_JSON);

//             Map<String, Object> body = new HashMap<>();
//             body.put("amount", transaction.getAmount()
//                     .multiply(BigDecimal.valueOf(100))
//                     .longValue());
//             body.put("email", transaction.getCustomerEmail() != null ? 
//                              transaction.getCustomerEmail() : "customer@example.com");
//             body.put("currency", transaction.getCurrency());
//             body.put("reference", transaction.getTransactionReference());
            
//             Map<String, String> metadata = new HashMap<>();
//             metadata.put("transactionReference", transaction.getTransactionReference());
//             metadata.put("tenantId", transaction.getTenantId());
//             body.put("metadata", metadata);

//             HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

//             ResponseEntity<Map> response = restTemplate.exchange(
//                     BASE_URL + "/transaction/initialize",
//                     HttpMethod.POST,
//                     entity,
//                     Map.class
//             );

//             if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                 Map<String, Object> responseBody = response.getBody();
//                 Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

//                 return ProcessorResponse.builder()
//                         .successful(true)
//                         .transactionId((String) data.get("reference"))
//                         .authorizationUrl((String) data.get("authorization_url"))
//                         .responseCode("00")
//                         .responseMessage("Payment initialized")
//                         .processedAt(java.time.LocalDateTime.now())
//                         .build();
//             }

//             return ProcessorResponse.builder()
//                     .successful(false)
//                     .responseMessage("Payment initialization failed")
//                     .build();

//         } catch (Exception e) {
//             log.error("Paystack payment failed", e);
//             return ProcessorResponse.builder()
//                     .successful(false)
//                     .responseMessage(e.getMessage())
//                     .build();
//         }
//     }

//     private ProcessorResponse simulatePaystackPayment(Transaction transaction) {
//         return ProcessorResponse.builder()
//                 .successful(true)
//                 .transactionId("ps_sim_" + transaction.getTransactionReference())
//                 .authorizationUrl("https://simulate.paystack.com/pay/" + transaction.getTransactionReference())
//                 .responseCode("00")
//                 .responseMessage("Simulated payment initialized")
//                 .processedAt(java.time.LocalDateTime.now())
//                 .build();
//     }

//     @Override
//     public ProcessorResponse getTransactionStatus(String processorTransactionId) {
//         if (secretKey == null || secretKey.isEmpty()) {
//             return ProcessorResponse.success(processorTransactionId);
//         }

//         try {
//             HttpHeaders headers = new HttpHeaders();
//             headers.setBearerAuth(secretKey);

//             HttpEntity<?> entity = new HttpEntity<>(headers);

//             ResponseEntity<Map> response = restTemplate.exchange(
//                     BASE_URL + "/transaction/verify/" + processorTransactionId,
//                     HttpMethod.GET,
//                     entity,
//                     Map.class
//             );

//             if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                 Map<String, Object> responseBody = response.getBody();
//                 Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
//                 String status = (String) data.get("status");

//                 return ProcessorResponse.builder()
//                         .successful("success".equals(status))
//                         .responseCode((String) data.get("gateway_response"))
//                         .responseMessage((String) data.get("message"))
//                         .build();
//             }

//             return ProcessorResponse.failure("Status check failed");

//         } catch (Exception e) {
//             return ProcessorResponse.failure(e.getMessage());
//         }
//     }

//     @Override
//     public String getProcessorName() {
//         return "PAYSTACK";
//     }
// }