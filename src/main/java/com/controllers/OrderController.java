package com.controllers;

import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderWithUserDto;
import com.enums.OrderStatus;
import com.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> getOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(orderService.getOrderById(id, authHeader));
    }

    @GetMapping
    public ResponseEntity<Page<OrderWithUserDto>> getAllOrders(
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end,
            Pageable pageable,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(
                orderService.getAllOrders(statuses, start, end, pageable, authHeader)
        );
    }

    @PostMapping
    public ResponseEntity<OrderWithUserDto> createOrder(
            @RequestBody @Valid OrderCreateUpdateDto dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.status(201)
                .body(orderService.createOrder(dto, authHeader));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> updateOrder(
            @PathVariable Long id,
            @RequestBody @Valid OrderCreateUpdateDto dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(orderService.updateOrder(id, dto, authHeader));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}