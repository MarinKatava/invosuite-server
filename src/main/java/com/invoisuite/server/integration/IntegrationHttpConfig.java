package com.invoisuite.server.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class IntegrationHttpConfig {

    /** Redirects are never followed: the URL was validated, whatever it redirects to was not. */
    @Bean
    public RestClient integrationRestClient(IntegrationProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory).build();
    }
}
