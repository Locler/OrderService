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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderItemServiceTest {

    @Mock
    private OrderItemRep orderItemRepository;

    @Mock
    private OrderRep orderRepository;

    @Mock
    private ItemRep itemRepository;

    @Mock
    private OrderItemMapper mapper;

    @InjectMocks
    private OrderItemService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createOrderItem_success_updatesTotal() {
        OrderItemCreateUpdateDto dto = OrderItemCreateUpdateDto.builder()
                .quantity(2)
                .orderId(1L)
                .itemId(2L)
                .build();

        Order order = new Order();
        order.setId(1L);
        order.setTotalPrice(BigDecimal.ZERO);

        Item item = new Item();
        item.setId(2L);
        item.setPrice(BigDecimal.valueOf(5));

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

        OrderItem entity = new OrderItem();
        entity.setOrder(order);
        entity.setItem(item);
        entity.setQuantity(2);

        when(mapper.fromCreateUpdateDto(dto)).thenReturn(entity);
        when(orderItemRepository.save(entity)).thenReturn(entity);
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(List.of(entity));
        when(mapper.toDto(entity)).thenReturn(new OrderItemDto());

        OrderItemDto res = service.createOrderItem(dto);
        assertThat(res).isNotNull();
        assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void createOrderItem_invalidDto_throws() {
        assertThatThrownBy(() -> service.createOrderItem(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateOrderItem_success_updatesTotal() {
        Long id = 7L;
        OrderItemCreateUpdateDto dto = OrderItemCreateUpdateDto.builder()
                .quantity(3)
                .orderId(1L)
                .itemId(2L)
                .build();

        OrderItem existing = new OrderItem();
        Order order = new Order();
        order.setId(1L);
        Item item = new Item();
        item.setPrice(BigDecimal.valueOf(2));
        existing.setOrder(order);
        existing.setItem(item);
        existing.setQuantity(1);

        when(orderItemRepository.findById(id)).thenReturn(Optional.of(existing));
        when(orderItemRepository.save(existing)).thenReturn(existing);
        when(orderItemRepository.findAllByOrderId(order.getId())).thenReturn(List.of(existing));
        when(mapper.toDto(existing)).thenReturn(new OrderItemDto());

        OrderItemDto res = service.updateOrderItem(id, dto);
        assertThat(res).isNotNull();
        verify(orderItemRepository).save(existing);
    }

    @Test
    void updateOrderItem_notFound_throws() {
        when(orderItemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateOrderItem(99L, OrderItemCreateUpdateDto.builder().quantity(1).orderId(1L).itemId(1L).build()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteOrderItem_success_updatesTotal() {
        OrderItem existing = new OrderItem();
        Order order = new Order();
        order.setId(1L);
        existing.setOrder(order);
        existing.setItem(new Item());
        when(orderItemRepository.findById(3L)).thenReturn(Optional.of(existing));
        doNothing().when(orderItemRepository).delete(existing);
        when(orderItemRepository.findAllByOrderId(order.getId())).thenReturn(List.of());

        service.deleteOrderItem(3L);
        verify(orderItemRepository).delete(existing);
    }

    @Test
    void getOrderItemById_success() {
        OrderItem existing = new OrderItem();
        when(orderItemRepository.findById(4L)).thenReturn(Optional.of(existing));
        OrderItemDto dto = new OrderItemDto();
        when(mapper.toDto(existing)).thenReturn(dto);

        OrderItemDto res = service.getOrderItemById(4L);
        assertThat(res).isEqualTo(dto);
    }

    @Test
    void getAllOrderItem_pagination() {
        OrderItem oi1 = new OrderItem();
        OrderItem oi2 = new OrderItem();
        Page<OrderItem> page = new PageImpl<>(List.of(oi1, oi2));
        Pageable pageable = PageRequest.of(0, 10);

        when(orderItemRepository.findAll(pageable)).thenReturn(page);
        when(mapper.toDto(oi1)).thenReturn(new OrderItemDto());
        when(mapper.toDto(oi2)).thenReturn(new OrderItemDto());

        Page<OrderItemDto> res = service.getAllOrderItem(pageable);
        assertThat(res.getContent()).hasSize(2);
    }
}
