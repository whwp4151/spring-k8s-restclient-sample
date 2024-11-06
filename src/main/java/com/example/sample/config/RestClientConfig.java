package com.example.sample.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.util.Collections;

@Configuration
@Slf4j
public class RestClientConfig {

    private static final String ORDER_BASE_URL = "http://order-service:8080";

    @Bean
    public SampleApi sampleApi() {
        return createHttpServiceProxy(ORDER_BASE_URL, SampleApi.class);
    }

    private <T> T createHttpServiceProxy(String baseUrl, Class<T> clientInterface) {
        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                })
                .defaultStatusHandler(HttpStatusCode::is2xxSuccessful, this::logExchange)
                .defaultStatusHandler(HttpStatusCode::isError, this::logExchange)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build();

        return factory.createClient(clientInterface);
    }

    private void logExchange(HttpRequest request, ClientHttpResponse response) throws IOException {
        log.info("[HTTP Exchange] {} {}", request.getMethod(), request.getURI());
        log.info("[Request Headers]");
        request.getHeaders().forEach((name, values) ->
                values.forEach(value -> log.info("  {} = {}", name, value))
        );
        log.info("[Response] Status: {}", response.getStatusCode());
    }

}
