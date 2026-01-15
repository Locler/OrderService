package com.services;

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

@Service
public class OrderItemService {

    private final OrderItemRep orderItemRepository;
    private final OrderItemMapper mapper;
    private final OrderRep orderRepository;
    private final ItemRep itemRepository;

    @Autowired
    public OrderItemService(OrderItemRep orderItemRepository, OrderItemMapper mapper, OrderRep orderRepository, ItemRep itemRepository) {
        this.orderItemRepository = orderItemRepository;
        this.mapper = mapper;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public OrderItemDto createOrderItem(OrderItemCreateUpdateDto dto) {
        validateCreateUpdateDto(dto);

        Order order = orderRepository.findByIdAndDeletedFalse(dto.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

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
    public OrderItemDto updateOrderItem(Long id, OrderItemCreateUpdateDto dto) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("OrderItem id must be positive");
        }
        validateCreateUpdateDto(dto);

        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("OrderItem not found"));

        orderItem.setQuantity(dto.getQuantity());

        orderItem = orderItemRepository.save(orderItem);

        updateOrderTotalPrice(orderItem.getOrder());

        return mapper.toDto(orderItem);
    }

    @Transactional
    public void deleteOrderItem(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("OrderItem id must be positive");
        }

        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("OrderItem not found"));

        Order order = orderItem.getOrder();

        orderItemRepository.delete(orderItem);

        updateOrderTotalPrice(order);
    }

    @Transactional(readOnly = true)
    public OrderItemDto getOrderItemById(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("OrderItem id must be positive");
        }

        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("OrderItem not found"));

        return mapper.toDto(orderItem);
    }

    @Transactional(readOnly = true)
    public Page<OrderItemDto> getAllOrderItem(Pageable pageable) {

        if (pageable == null) {
            throw new IllegalArgumentException("Pageable must be provided");
        }

        return orderItemRepository.findAll(pageable)
                .map(mapper::toDto);
    }

    private void validateCreateUpdateDto(OrderItemCreateUpdateDto dto) {

        if (dto == null) {
            throw new IllegalArgumentException("OrderItemCreateUpdateDto cannot be null");
        }
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }
        if (dto.getOrderId() == null || dto.getOrderId() <= 0) {
            throw new IllegalArgumentException("Order id must be positive");
        }
        if (dto.getItemId() == null || dto.getItemId() <= 0) {
            throw new IllegalArgumentException("Item id must be positive");
        }
    }

    private void updateOrderTotalPrice(Order order) {
        if (order == null) return;

        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());

        BigDecimal total = items.stream()
                .filter(oi -> oi.getItem() != null && oi.getQuantity() != null)
                .map(oi -> oi.getItem().getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(total);
    }
}
