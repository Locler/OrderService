package com.services;

import com.checker.AccessChecker;
import com.dtos.UserInfoDto;
import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderWithUserDto;
import com.entities.Order;
import com.enums.OrderStatus;
import com.mappers.OrderMapper;
import com.repositories.OrderRep;
import com.specifications.OrderServiceSpecifications;
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
import java.util.Set;

@Service
public class OrderService {

    private final OrderRep orderRepository;
    private final OrderMapper mapper;
    private final UserServiceClient userServiceClient;
    private final AccessChecker accessChecker;

    @Autowired
    public OrderService(OrderRep orderRepository, OrderMapper mapper, UserServiceClient userServiceClient, AccessChecker accessChecker) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
        this.userServiceClient = userServiceClient;
        this.accessChecker = accessChecker;
    }


    @Transactional
    public OrderWithUserDto createOrder(OrderCreateUpdateDto dto, Long requesterId, Set<String> roles) {
        // Проверка: USER может создавать только свои заказы
        accessChecker.checkUserAccess(requesterId, requesterId, roles);

        if (dto == null || dto.getStatus() == null || dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Invalid order DTO");
        }

        // Получаем пользователя по email из UserService
        UserInfoDto user = userServiceClient.getUserByEmail(dto.getEmail(), requesterId, roles);

        if (user == null || !user.getActive()) {
            throw new IllegalStateException("Cannot create order for inactive or unknown user");
        }

        Order order = mapper.fromCreateUpdateDto(dto);
        order.setUserId(user.getId());
        order.setDeleted(false);
        order.setTotalPrice(BigDecimal.ZERO);

        order = orderRepository.save(order);

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

        // Ограничиваем обычного USER только своими заказами
        if (!roles.contains("ADMIN")) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), requesterId));
        }

        Page<Order> page = orderRepository.findAll(spec, pageable);

        return page.map(order -> {
            UserInfoDto user = userServiceClient.getUserById(order.getUserId(), requesterId, roles);
            return new OrderWithUserDto(mapper.toDto(order), user);
        });
    }

    @Transactional
    public OrderWithUserDto updateOrder(Long id, OrderCreateUpdateDto dto, Long requesterId, Set<String> roles) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        accessChecker.checkUserAccess(order.getUserId(), requesterId, roles);

        order.setStatus(dto.getStatus());
        order = orderRepository.save(order);

        UserInfoDto user = userServiceClient.getUserById(order.getUserId(), requesterId, roles);
        return new OrderWithUserDto(mapper.toDto(order), user);
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
