package com.services;

import com.dtos.UserInfoDto;
import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderWithUserDto;
import com.entities.Order;
import com.enums.OrderStatus;
import com.mappers.OrderMapper;
import com.repositories.OrderRep;
import com.specifications.OrderServiceSpecifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class OrderService {

    private final OrderRep orderRepository;
    private final OrderMapper mapper;
    private final UserServiceClient userServiceClient;

    @Autowired
    public OrderService(OrderRep orderRepository, OrderMapper mapper, UserServiceClient userServiceClient) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public OrderWithUserDto createOrder(OrderCreateUpdateDto dto, String authHeader) {

        if (dto == null) {
            throw new IllegalArgumentException("OrderCreateUpdateDto cannot be null");
        }
        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("Order status must be provided");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email must be provided");
        }


        UserInfoDto user = userServiceClient.getUserByEmail(dto.getEmail(), authHeader);

        log.info("User from UserService: {}", user);
        log.info("User ID: {}", user != null ? user.getId() : null);

        if (user == null || user.getEmail() == null) {
            throw new RuntimeException("Cannot retrieve user info from UserService");
        }

        Order order = mapper.fromCreateUpdateDto(dto);
        order.setUserId(user.getId());
        order.setDeleted(false);
        order.setTotalPrice(BigDecimal.ZERO);

        order = orderRepository.save(order);

        return new OrderWithUserDto(mapper.toDto(order), user);
    }

    @Transactional(readOnly = true)
    public OrderWithUserDto getOrderById(Long id, String authHeader) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Order id must be positive");
        }

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        UserInfoDto user = userServiceClient.getUserById(order.getUserId(), authHeader);
        if (user == null) {
            throw new RuntimeException("Cannot retrieve user info from UserService");
        }

        return new OrderWithUserDto(mapper.toDto(order), user);
    }

    @Transactional(readOnly = true)
    public Page<OrderWithUserDto> getAllOrders(
            List<OrderStatus> statuses,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable,
            String authHeader
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable must be provided");
        }

        Specification<Order> spec = OrderServiceSpecifications.notDeleted();

        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and(OrderServiceSpecifications.hasStatuses(statuses));
        }

        if (start != null && end != null) {
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("Start date must be before end date");
            }
            spec = spec.and(OrderServiceSpecifications.createdBetween(start, end));
        }

        Page<Order> ordersPage = orderRepository.findAll(spec, pageable);

        return ordersPage.map(order -> {
            UserInfoDto user = userServiceClient.getUserById(order.getUserId(), authHeader);
            if (user == null) {
                user = UserInfoDto.builder()
                        .name("Unknown")
                        .surname("User")
                        .email("unavailable")
                        .active(false)
                        .birthDate(null)
                        .build();
            }
            return new OrderWithUserDto(mapper.toDto(order), user);
        });
    }

    @Transactional
    public OrderWithUserDto updateOrder(Long id, OrderCreateUpdateDto dto, String authHeader) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Order id must be positive");
        }
        if (dto == null) {
            throw new IllegalArgumentException("OrderCreateUpdateDto cannot be null");
        }
        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("Order status must be provided");
        }

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        order.setStatus(dto.getStatus());
        order = orderRepository.save(order);

        UserInfoDto user = userServiceClient.getUserById(order.getUserId(), authHeader);
        if (user == null) {
            user = UserInfoDto.builder()
                    .name("Unknown")
                    .surname("User")
                    .email("unavailable")
                    .active(false)
                    .birthDate(null)
                    .build();
        }

        return new OrderWithUserDto(mapper.toDto(order), user);
    }

    @Transactional
    public void deleteOrder(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Order id must be positive");
        }

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        order.setDeleted(true);
        orderRepository.save(order);
    }
}
