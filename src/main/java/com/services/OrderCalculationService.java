package com.services;

import com.entities.Order;
import com.entities.OrderItem;
import com.repositories.OrderItemRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderCalculationService {

    private final OrderItemRep orderItemRepository;

    @Autowired
    public OrderCalculationService(OrderItemRep orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    public BigDecimal calculateTotal(Long orderId) {
        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId);
        return items.stream()
                .map(oi -> oi.getItem().getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void updateTotal(Order order) {
        if (order == null || order.getId() == null) return;

        BigDecimal total = calculateTotal(order.getId());
        order.setTotalPrice(total);
    }

}
