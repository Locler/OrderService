package com.services;

import com.checker.AccessChecker;
import com.dtos.request.OrderItemCreateUpdateDto;
import com.dtos.response.OrderItemDto;
import com.entities.Item;
import com.entities.Order;
import com.entities.OrderItem;
import com.mappers.OrderItemMapper;
import com.repositories.ItemRep;
import com.repositories.OrderItemRep;
import com.repositories.OrderRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class OrderItemService {

    private final OrderItemRep orderItemRepository;
    private final OrderItemMapper mapper;
    private final OrderRep orderRepository;
    private final ItemRep itemRepository;
    private final AccessChecker accessChecker;

    @Autowired
    public OrderItemService(OrderItemRep orderItemRepository, OrderItemMapper mapper, OrderRep orderRepository, ItemRep itemRepository, AccessChecker accessChecker) {
        this.orderItemRepository = orderItemRepository;
        this.mapper = mapper;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.accessChecker = accessChecker;
    }

    @Transactional
    public OrderItemDto createOrderItem(OrderItemCreateUpdateDto dto, Long requesterId, Set<String> roles) {
        validateCreateUpdateDto(dto);

        Order order = orderRepository.findByIdAndDeletedFalse(dto.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        accessChecker.checkUserAccess(order.getUserId(), requesterId, roles);

        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NoSuchElementException("Item not found"));

        OrderItem orderItem = mapper.fromCreateUpdateDto(dto);
        orderItem.setOrder(order);
        orderItem.setItem(item);

        orderItem = orderItemRepository.save(orderItem);

        updateOrderTotalPrice(order);
        return mapper.toDto(orderItem);
    }

    @Transactional
    public OrderItemDto updateOrderItem(Long id, OrderItemCreateUpdateDto dto, Long requesterId, Set<String> roles) {
        validateCreateUpdateDto(dto);

        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("OrderItem not found"));

        accessChecker.checkUserAccess(orderItem.getOrder().getUserId(), requesterId, roles);

        orderItem.setQuantity(dto.getQuantity());
        orderItem = orderItemRepository.save(orderItem);

        updateOrderTotalPrice(orderItem.getOrder());
        return mapper.toDto(orderItem);
    }

    @Transactional
    public void deleteOrderItem(Long id, Long requesterId, Set<String> roles) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("OrderItem not found"));

        accessChecker.checkUserAccess(orderItem.getOrder().getUserId(), requesterId, roles);

        Order order = orderItem.getOrder();
        orderItemRepository.delete(orderItem);

        updateOrderTotalPrice(order);
    }

    @Transactional(readOnly = true)
    public OrderItemDto getOrderItemById(Long id, Long requesterId, Set<String> roles) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("OrderItem not found"));

        accessChecker.checkUserAccess(orderItem.getOrder().getUserId(), requesterId, roles);
        return mapper.toDto(orderItem);
    }

    @Transactional(readOnly = true)
    public Page<OrderItemDto> getAllOrderItems(Pageable pageable, Set<String> roles) {
        if (!roles.contains("ADMIN")) {
            throw new IllegalArgumentException("Only admin can list all order items");
        }
        return orderItemRepository.findAll(pageable).map(mapper::toDto);
    }

    private void validateCreateUpdateDto(OrderItemCreateUpdateDto dto) {
        if (dto == null || dto.getQuantity() == null || dto.getQuantity() < 1 ||
                dto.getOrderId() == null || dto.getOrderId() <= 0 ||
                dto.getItemId() == null || dto.getItemId() <= 0) {
            throw new IllegalArgumentException("Invalid OrderItem DTO");
        }
    }

    private void updateOrderTotalPrice(Order order) {
        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        BigDecimal total = items.stream()
                .map(oi -> oi.getItem().getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalPrice(total);
        orderRepository.save(order);
    }
}
