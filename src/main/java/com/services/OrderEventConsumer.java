package com.services;

import com.enums.OrderStatus;
import com.event.CreatePaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "create-payment", groupId = "order-service-group")
    public void handleCreatePaymentEvent(CreatePaymentEvent event) {
        log.info("Received CREATE_PAYMENT event: {}", event);

        try {
            switch (event.getStatus()) {
                case SUCCESS -> orderService.updateStatus(event.getOrderId(), OrderStatus.COMPLETED,
                        0L, Set.of("SYSTEM")); // SYSTEM user для автоматической обработки
                case FAILED -> orderService.updateStatus(event.getOrderId(), OrderStatus.CANCELLED,
                        0L, Set.of("SYSTEM"));
                case NEW -> log.info("Payment NEW status, no order update needed");
            }
        } catch (Exception e) {
            log.error("Failed to update order status for orderId {}: {}", event.getOrderId(), e.getMessage());
        }
    }
}
