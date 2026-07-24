package com.yatayat.backend.payment;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class EsewaHttpClient implements EsewaGateway {
    private final EsewaProperties properties;
    private final RestClient client;

    public EsewaHttpClient(EsewaProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public StatusResult lookup(String productCode, String totalAmount, String transactionUuid) {
        String url = UriComponentsBuilder.fromUriString(properties.getStatusUrl())
                .queryParam("product_code", productCode)
                .queryParam("total_amount", totalAmount)
                .queryParam("transaction_uuid", transactionUuid)
                .build().encode().toUriString();
        try {
            StatusResponse response = client.get().uri(url).retrieve().body(StatusResponse.class);
            if (response == null || blank(response.status())) {
                throw providerError("eSewa returned an incomplete status response.");
            }
            return new StatusResult(response.productCode(), response.transactionUuid(),
                    response.totalAmount(), response.status(), response.referenceId());
        } catch (RestClientResponseException exception) {
            throw providerError("eSewa rejected the status request.");
        } catch (ResourceAccessException exception) {
            throw providerError("eSewa is temporarily unavailable.");
        } catch (RestClientException exception) {
            throw providerError("eSewa returned an invalid status response.");
        }
    }

    private ResponseStatusException providerError(String message) {
        return new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, message);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }

    private record StatusResponse(
            @JsonAlias({"product_code", "scd"}) String productCode,
            @JsonAlias({"transaction_uuid", "pid"}) String transactionUuid,
            @JsonAlias({"total_amount", "totalAmount"}) java.math.BigDecimal totalAmount,
            String status,
            @JsonAlias({"ref_id", "refId"}) String referenceId
    ) {}
}
