package com.controllers;

import com.dtos.request.OrderItemCreateUpdateDto;
import com.dtos.response.OrderItemDto;
import com.services.OrderItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orderItems")
@RequiredArgsConstructor
public class OrderItemController {

    private final OrderItemService service;

    @GetMapping("/{id}")
    public ResponseEntity<OrderItemDto> getOrderItem(@PathVariable Long id) {
        return ResponseEntity.ok(service.getOrderItemById(id));
    }

    @GetMapping
    public ResponseEntity<Page<OrderItemDto>> getAllOrderItems(Pageable pageable) {
        return ResponseEntity.ok(service.getAllOrderItem(pageable));
    }

    @PostMapping
    public ResponseEntity<OrderItemDto> createOrderItem(@RequestBody @Valid OrderItemCreateUpdateDto dto) {
        return ResponseEntity.status(201).body(service.createOrderItem(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderItemDto> updateOrderItem(
            @PathVariable Long id,
            @RequestBody @Valid OrderItemCreateUpdateDto dto
    ) {
        return ResponseEntity.ok(service.updateOrderItem(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderItem(@PathVariable Long id) {
        service.deleteOrderItem(id);
        return ResponseEntity.noContent().build();
    }
}
