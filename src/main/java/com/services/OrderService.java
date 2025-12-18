package com.services;

import com.checker.AccessChecker;
import com.dtos.UserInfoDto;
import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderWithUserDto;
import com.entities.Order;
import com.entities.OrderItem;
import com.enums.OrderStatus;
import com.fsm.OrderStatusTransitions;
import com.mappers.OrderItemMapper;
import com.mappers.OrderMapper;
import com.repositories.OrderRep;
import com.specifications.OrderServiceSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class OrderService {

    private final OrderRep orderRepository;
    private final OrderMapper mapper;
    private final UserServiceClient userServiceClient;
    private final AccessChecker accessChecker;
    private final OrderItemMapper orderItemMapper;
    private final OrderCalculationService orderCalculationService;

    @Autowired
    public OrderService(OrderRep orderRepository, OrderMapper mapper, UserServiceClient userServiceClient, AccessChecker accessChecker, OrderItemMapper orderItemMapper, OrderCalculationService orderCalculationService) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
        this.userServiceClient = userServiceClient;
        this.accessChecker = accessChecker;
        this.orderItemMapper = orderItemMapper;
        this.orderCalculationService = orderCalculationService;
    }

    @Transactional
    public OrderWithUserDto createOrder(OrderCreateUpdateDto dto, Long requesterId, Set<String> roles) {

        accessChecker.checkUserAccess(requesterId, requesterId, roles);

        UserInfoDto user = userServiceClient.getUserById(requesterId, requesterId, roles);
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Cannot create order for inactive or unknown user");
        }

        Order orderEntity = mapper.fromCreateUpdateDto(dto);
        orderEntity.setUserId(user.getId());
        orderEntity.setStatus(OrderStatus.NEW);
        orderEntity.setDeleted(false);


        orderEntity = orderRepository.save(orderEntity);

        List<OrderItem> items = orderItemMapper.fromDtoList(dto.getOrderItems());
        for (OrderItem item : items) {
            item.setOrder(orderEntity);
        }
        orderEntity.setOrderItems(items);

        orderEntity = orderRepository.save(orderEntity);

        // Пересчёт totalPrice
        orderCalculationService.updateTotal(orderEntity);

        return new OrderWithUserDto(mapper.toDto(orderEntity), user);
    }

    @Transactional
    public OrderWithUserDto updateStatus(Long id, OrderStatus newStatus,
                                         Long requesterId, Set<String> roles) {

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        // Только ADMIN
        accessChecker.checkAdminAccess(roles);

        OrderStatus current = order.getStatus();

        if (!OrderStatusTransitions.canTransition(current, newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition: " + current + " → " + newStatus
            );
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        UserInfoDto user = userServiceClient.getUserById(order.getUserId(), requesterId, roles);
        return new OrderWithUserDto(mapper.toDto(order), user);
    }

    @Transactional(readOnly = true)
    public OrderWithUserDto getOrderById(Long id, Long requesterId, Set<String> roles) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        accessChecker.checkUserAccess(order.getUserId(), requesterId, roles);

        UserInfoDto user = userServiceClient.getUserById(order.getUserId(), requesterId, roles);
        return new OrderWithUserDto(mapper.toDto(order), user);
    }

    @Transactional(readOnly = true)
    public Page<OrderWithUserDto> getAllOrders(List<OrderStatus> statuses, LocalDateTime start, LocalDateTime end,
                                               Pageable pageable, Long requesterId, Set<String> roles) {

        Specification<Order> spec = OrderServiceSpecifications.notDeleted();

        if (statuses != null && !statuses.isEmpty()) spec = spec.and(OrderServiceSpecifications.hasStatuses(statuses));
        if (start != null && end != null && !start.isAfter(end)) spec = spec.and(OrderServiceSpecifications.createdBetween(start, end));

        accessChecker.checkAdminAccess(roles);

        Page<Order> page = orderRepository.findAll(spec, pageable);

        return page.map(order -> {
            UserInfoDto user = userServiceClient.getUserById(order.getUserId(), requesterId, roles);
            return new OrderWithUserDto(mapper.toDto(order), user);
        });
    }

    @Transactional
    public OrderWithUserDto updateOrder(Long id, OrderCreateUpdateDto dto, Long requesterId, Set<String> roles) {

        Order orderEntity = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        accessChecker.checkUserAccess(orderEntity.getUserId(), requesterId, roles);

        orderEntity.getOrderItems().clear();
        List<OrderItem> items = orderItemMapper.fromDtoList(dto.getOrderItems());
        for (OrderItem item : items) {
            item.setOrder(orderEntity);
        }
        orderEntity.setOrderItems(items);

        orderEntity = orderRepository.save(orderEntity);

        orderCalculationService.updateTotal(orderEntity);

        UserInfoDto user = userServiceClient.getUserById(orderEntity.getUserId(), requesterId, roles);
        return new OrderWithUserDto(mapper.toDto(orderEntity), user);
    }

    @Transactional
    public void deleteOrder(Long id, Long requesterId, Set<String> roles) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        accessChecker.checkUserAccess(order.getUserId(), requesterId, roles);

        order.setDeleted(true);
        orderRepository.save(order);
    }
}
