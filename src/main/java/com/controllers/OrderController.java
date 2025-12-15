package com.controllers;

import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderWithUserDto;
import com.enums.OrderStatus;
import com.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        return Arrays.stream(rolesHeader.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> getOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(orderService.getOrderById(id, requesterId, roles));
    }

    @GetMapping
    public ResponseEntity<Page<OrderWithUserDto>> getAllOrders(
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end,
            @PageableDefault Pageable pageable,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(
                orderService.getAllOrders(statuses, start, end, pageable, requesterId, roles)
        );
    }

    @PostMapping
    public ResponseEntity<OrderWithUserDto> createOrder(
            @RequestBody @Valid OrderCreateUpdateDto dto,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.status(201)
                .body(orderService.createOrder(dto, requesterId, roles));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> updateOrder(
            @PathVariable Long id,
            @RequestBody @Valid OrderCreateUpdateDto dto,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(orderService.updateOrder(id, dto, requesterId, roles));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        orderService.deleteOrder(id, requesterId, roles);
        return ResponseEntity.noContent().build();
    }
}