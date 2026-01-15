package com.controllers;

import com.dtos.request.OrderItemCreateUpdateDto;
import com.dtos.response.OrderItemDto;
import com.services.OrderItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orderItems")
@RequiredArgsConstructor
public class OrderItemController {

    private final OrderItemService service;

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        return Arrays.stream(rolesHeader.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderItemDto> getOrderItem(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(service.getOrderItemById(id, requesterId, roles));
    }

    @GetMapping
    public ResponseEntity<Page<OrderItemDto>> getAllOrderItems(
            @PageableDefault Pageable pageable,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(service.getAllOrderItems(pageable, roles));
    }

    @PostMapping
    public ResponseEntity<OrderItemDto> createOrderItem(
            @RequestBody @Valid OrderItemCreateUpdateDto dto,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.status(201).body(service.createOrderItem(dto, requesterId, roles));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderItemDto> updateOrderItem(
            @PathVariable Long id,
            @RequestBody @Valid OrderItemCreateUpdateDto dto,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(service.updateOrderItem(id, dto, requesterId, roles));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderItem(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        service.deleteOrderItem(id, requesterId, roles);
        return ResponseEntity.noContent().build();
    }
}
