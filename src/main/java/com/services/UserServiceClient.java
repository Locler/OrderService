package com.services;

import com.dtos.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
                .onStatus(
                        HttpStatusCode::isError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    System.out.println("UserService returned error: " + errorBody);
                                    return new RuntimeException("UserService error: " + errorBody);
                                })
                )
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
        try {
            WebClient client = webClientBuilder
                    .baseUrl(userServiceBaseUrl)
                    .build();

            UserInfoDto user = client.get()
                    .uri("/{id}", userId)
                    .header("X-User-Id", requesterId.toString())
                    .header("X-User-Roles", String.join(",", roles))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        System.out.println("UserService returned error: " + errorBody);
                                        return new RuntimeException("UserService error: " + errorBody);
                                    })
                    )
                    .bodyToMono(UserInfoDto.class)
                    .block();

            if (user == null) {
                throw new IllegalStateException("User not found");
            }

            return user;

        } catch (WebClientResponseException ex) {
            // Любые реальные ошибки HTTP (5xx) → CircuitBreaker сработает
            throw ex;
        } catch (Exception ex) {
            throw ex;
        }
    }

    public UserInfoDto fallbackUserById(Long userId, Long requesterId, Set<String> roles, Throwable ex) {
        // Fallback срабатывает только если сервис реально недоступен
        return UserInfoDto.builder()
                .name("Unknown")
                .surname("User")
                .email("unavailable")
                .active(false)
                .birthDate(null)
                .build();
    }
}