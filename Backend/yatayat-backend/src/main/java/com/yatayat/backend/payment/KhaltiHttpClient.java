package com.yatayat.backend.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class KhaltiHttpClient implements KhaltiGateway {
    private static final Logger log = LoggerFactory.getLogger(KhaltiHttpClient.class);
    private final KhaltiProperties properties;
    private final RestClient client;
    private final ObjectMapper objectMapper;

    public KhaltiHttpClient(KhaltiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public InitiationResult initiate(InitiationRequest request) {
        requireEnabled();
        Map<String, Object> payload = Map.of(
                "return_url", request.returnUrl(), "website_url", request.websiteUrl(),
                "amount", request.amount(), "purchase_order_id", request.purchaseOrderId(),
                "purchase_order_name", request.purchaseOrderName(),
                "customer_info", Map.of("name", safe(request.customerInfo().name()),
                        "email", safe(request.customerInfo().email()),
                        "phone", safe(request.customerInfo().phone())));
        InitiationResponse response = post(properties.getInitiateUrl(), payload, InitiationResponse.class);
        if (response == null || blank(response.pidx()) || blank(response.payment_url()))
            throw providerError("Khalti returned an incomplete initiation response.");
        return new InitiationResult(response.pidx(), response.payment_url(), response.expires_at(), response.expires_in());
    }

    @Override
    public LookupResult lookup(String pidx) {
        requireEnabled();
        LookupResponse response = post(properties.getLookupUrl(), Map.of("pidx", pidx), LookupResponse.class);
        if (response == null || blank(response.pidx()) || blank(response.status()))
            throw providerError("Khalti returned an incomplete lookup response.");
        return new LookupResult(response.pidx(), response.total_amount(), response.status(),
                response.transaction_id(), response.refunded());
    }

    private <T> T post(String url, Object body, Class<T> responseType) {
        try {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("Khalti request URL: {}", url);
            log.info("Khalti request method: POST");
            log.info("Khalti Authorization header: Key [REDACTED]");
            log.info("Khalti request JSON body: {}", requestJson);

            var response = client.post().uri(url).header("Authorization", "Key " + properties.getSecretKey())
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(requestJson).retrieve().toEntity(String.class);
            log.info("Khalti response HTTP status: {}", response.getStatusCode().value());
            log.info("Khalti response body: {}", response.getBody());
            return objectMapper.readValue(response.getBody(), responseType);
        } catch (RestClientResponseException exception) {
            log.error("Khalti response HTTP status: {}", exception.getStatusCode().value());
            log.error("Khalti response body: {}", exception.getResponseBodyAsString());
            log.error("Khalti initiate/lookup request failed", exception);
            throw providerError("Khalti rejected the request.");
        } catch (ResourceAccessException exception) {
            log.error("Khalti initiate/lookup request failed", exception);
            throw providerError("Khalti is temporarily unavailable.");
        } catch (RestClientException exception) {
            log.error("Khalti initiate/lookup request failed", exception);
            throw providerError("Khalti returned an invalid response.");
        } catch (JsonProcessingException exception) {
            log.error("Khalti initiate/lookup JSON processing failed", exception);
            throw providerError("Khalti returned an invalid response.");
        }
    }
    private void requireEnabled() { if (!properties.isEnabled()) throw providerError("Khalti payments are not configured."); }
    private ResponseStatusException providerError(String message) {
        return new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, message);
    }
    private String safe(String value) { return value == null ? "" : value.trim(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private record InitiationResponse(String pidx, String payment_url, String expires_at, Long expires_in) {}
    private record LookupResponse(String pidx, Long total_amount, String status,
                                  String transaction_id, Boolean refunded) {}
}
