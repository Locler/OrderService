package com.services;

import com.entities.Item;
import com.entities.Order;
import com.entities.OrderItem;
import com.repositories.ItemRep;
import com.repositories.OrderItemRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderCalculationService {

    private final OrderItemRep orderItemRepository;
    private final ItemRep itemRepository;

    @Autowired
    public OrderCalculationService(OrderItemRep orderItemRepository, ItemRep itemRepository) {
        this.orderItemRepository = orderItemRepository;
        this.itemRepository = itemRepository;
    }


    public BigDecimal calculateTotal(Long orderId) {
        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId);
        return items.stream()
                .map(orderItem -> {
                    Long itemId = orderItem.getItem().getId();
                    Item item = itemRepository.findById(itemId)
                            .orElseThrow(() -> new IllegalStateException("Item not found: " + itemId));
                    return item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void updateTotal(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            order.setTotalPrice(BigDecimal.ZERO);
            return;
        }

        BigDecimal total = order.getOrderItems().stream()
                .map(orderItem -> {
                    Long itemId = orderItem.getItem().getId();
                    Item item = itemRepository.findById(itemId)
                            .orElseThrow(() -> new IllegalStateException("Item not found: " + itemId));
                    return item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(total);
    }
}
