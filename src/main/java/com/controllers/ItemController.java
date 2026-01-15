package com.controllers;

import com.dtos.request.ItemCreateUpdateDto;
import com.dtos.response.ItemDto;
import com.services.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        return Arrays.stream(rolesHeader.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItem(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(itemService.getItem(id, requesterId, roles));
    }

    @GetMapping
    public ResponseEntity<Page<ItemDto>> getAllItems(
            Pageable pageable,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(itemService.getAllItems(pageable, requesterId, roles));
    }

    @PostMapping
    public ResponseEntity<ItemDto> createItem(
            @RequestBody @Valid ItemCreateUpdateDto dto,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.status(201).body(itemService.createItem(dto, roles));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable Long id,
            @RequestBody @Valid ItemCreateUpdateDto dto,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(itemService.updateItem(id, dto, roles));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        itemService.deleteItem(id,roles);
        return ResponseEntity.noContent().build();
    }
}
