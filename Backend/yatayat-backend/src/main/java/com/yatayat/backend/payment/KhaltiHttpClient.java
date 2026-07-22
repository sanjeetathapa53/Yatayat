package com.yatayat.backend.payment;

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
    private final KhaltiProperties properties;
    private final RestClient client;

    public KhaltiHttpClient(KhaltiProperties properties) {
        this.properties = properties;
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
            return client.post().uri(url).header("Authorization", "Key " + properties.getSecretKey())
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(responseType);
        } catch (RestClientResponseException exception) {
            throw providerError("Khalti rejected the request.");
        } catch (ResourceAccessException exception) {
            throw providerError("Khalti is temporarily unavailable.");
        } catch (RestClientException exception) {
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
