package com.unitTests;

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
import com.services.OrderCalculationService;
import com.services.OrderItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderItemServiceTest {

    @Mock
    private OrderItemRep orderItemRep;
    @Mock
    private OrderRep orderRep;
    @Mock
    private ItemRep itemRep;
    @Mock
    private OrderItemMapper mapper;
    @Mock
    private AccessChecker accessChecker;
    @Mock
    private OrderCalculationService orderCalculationService;

    @InjectMocks
    private OrderItemService service;

    private Order order;
    private Item item;
    private OrderItem orderItem;
    private OrderItemCreateUpdateDto dto;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setOrderItems(new ArrayList<>());

        item = new Item();
        item.setId(1L);
        item.setPrice(java.math.BigDecimal.TEN);

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        orderItem.setItem(item);
        orderItem.setQuantity(2);

        dto = OrderItemCreateUpdateDto.builder()
                .orderId(order.getId())
                .itemId(item.getId())
                .quantity(2)
                .build();
    }

    @Test
    void createOrderItem() {
        when(orderRep.findByIdAndDeletedFalse(order.getId())).thenReturn(Optional.of(order));
        when(itemRep.findById(item.getId())).thenReturn(Optional.of(item));
        when(mapper.fromCreateUpdateDto(dto)).thenReturn(orderItem);
        when(mapper.toDto(orderItem)).thenReturn(mock(OrderItemDto.class));

        service.createOrderItem(dto, 1L, Set.of("ROLE_USER"));

        verify(orderItemRep).save(orderItem);
        verify(orderCalculationService).updateTotal(order);
    }

    @Test
    void updateOrderItem() {
        when(orderItemRep.findById(1L)).thenReturn(Optional.of(orderItem));
        when(mapper.toDto(orderItem)).thenReturn(mock(OrderItemDto.class));

        service.updateOrderItem(1L, dto, 1L, Set.of("ROLE_USER"));

        assertEquals(2, orderItem.getQuantity());
        verify(orderItemRep).save(orderItem);
    }

    @Test
    void deleteOrderItem() {
        when(orderItemRep.findById(1L)).thenReturn(Optional.of(orderItem));

        service.deleteOrderItem(1L, 1L, Set.of("ROLE_USER"));

        verify(orderItemRep).delete(orderItem);
        verify(orderCalculationService).updateTotal(order);
    }

    @Test
    void getOrderItemById() {
        when(orderItemRep.findById(1L)).thenReturn(Optional.of(orderItem));
        when(mapper.toDto(orderItem)).thenReturn(mock(OrderItemDto.class));

        OrderItemDto dtoResult = service.getOrderItemById(1L, 1L, Set.of("ROLE_USER"));
        assertNotNull(dtoResult);
    }
}
