package com.services;

import com.dtos.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user.service.base-url}")
    private String userServiceBaseUrl;

    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "fallbackUser")
    public UserInfoDto getUserByEmail(String email, Long requesterId, Set<String> roles) {
        WebClient client = webClientBuilder.baseUrl(userServiceBaseUrl).build();

        return client.get()
                .uri(uriBuilder -> uriBuilder.path("/by-email").queryParam("email", email).build())
                .header("X-User-Id", requesterId.toString())
                .header("X-User-Roles", String.join(",", roles))
                .retrieve()
                .bodyToMono(UserInfoDto.class)
                .block();
    }

    public UserInfoDto fallbackUser(String email, Long requesterId, Set<String> roles, Throwable ex) {
        return UserInfoDto.builder()
                .name("Unknown")
                .surname("User")
                .email(email)
                .active(false)
                .birthDate(null)
                .build();
    }

    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "fallbackUserById")
    public UserInfoDto getUserById(Long userId, Long requesterId, Set<String> roles) {
        WebClient client = webClientBuilder.baseUrl(userServiceBaseUrl).build();

        return client.get()
                .uri("/{id}", userId)
                .header("X-User-Id", requesterId.toString())
                .header("X-User-Roles", String.join(",", roles))
                .retrieve()
                .bodyToMono(UserInfoDto.class)
                .block();
    }

    public UserInfoDto fallbackUserById(Long userId, Long requesterId, Set<String> roles, Throwable ex) {
        return UserInfoDto.builder()
                .name("Unknown")
                .surname("User")
                .email("unavailable")
                .active(false)
                .birthDate(null)
                .build();
    }
}