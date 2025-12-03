package com.services;

import com.dtos.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user.service.base-url}")
    private String userServiceBaseUrl;

    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "fallbackUser")
    public UserInfoDto getUserByEmail(String email, String token) {
        return webClientBuilder.build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(userServiceBaseUrl + "/by-email")
                        .queryParam("email", email)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(UserInfoDto.class)
                .block();
    }

    public UserInfoDto fallbackUser(String email, String token, Throwable ex) {
        return UserInfoDto.builder()
                .name("Unknown")
                .surname("User")
                .email(email)
                .active(false)
                .birthDate(null)
                .build();
    }

    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "fallbackUserById")
    public UserInfoDto getUserById(Long userId, String token) {
        return webClientBuilder.build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(userServiceBaseUrl + "/{id}")
                        .build(userId))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(UserInfoDto.class)
                .block();
    }

    public UserInfoDto fallbackUserById(Long userId, String token, Throwable ex) {
        return UserInfoDto.builder()
                .name("Unknown")
                .surname("User")
                .email("unavailable")
                .active(false)
                .birthDate(null)
                .build();
    }
}