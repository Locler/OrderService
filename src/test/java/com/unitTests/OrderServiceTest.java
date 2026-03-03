package com.unitTests;

import com.checker.AccessChecker;
import com.dtos.UserInfoDto;
import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderDto;
import com.dtos.response.OrderItemDto;
import com.dtos.response.OrderWithUserDto;
import com.entities.Item;
import com.entities.Order;
import com.entities.OrderItem;
import com.enums.OrderStatus;
import com.mappers.OrderItemMapper;
import com.mappers.OrderMapper;
import com.repositories.ItemRep;
import com.repositories.OrderRep;
import com.services.OrderCalculationService;
import com.services.OrderService;
import com.services.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRep orderRepository;
    @Mock
    private ItemRep itemRepository;
    @Mock
    private OrderMapper mapper;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private AccessChecker accessChecker;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private OrderCalculationService orderCalculationService;

    @InjectMocks
    private OrderService orderService;

    private final Long USER_ID = 1L;
    private final Set<String> USER_ROLES = Set.of("ROLE_USER");
    private final Set<String> ADMIN_ROLES = Set.of("ROLE_ADMIN");


    @Test
    void createOrder() {
        OrderCreateUpdateDto dto = new OrderCreateUpdateDto();
        dto.setOrderItems(List.of(new OrderItemDto(1L, 2, 1L, 1L)));

        UserInfoDto user = UserInfoDto.builder()
                .id(USER_ID)
                .active(true)
                .build();

        Order order = new Order();
        order.setId(1L);

        OrderDto orderDto = OrderDto.builder()
                .id(1L)
                .status(OrderStatus.NEW)
                .totalPrice(BigDecimal.TEN)
                .build();

        OrderItem item = new OrderItem();

        when(userServiceClient.getUserById(any(), any(), any())).thenReturn(user);
        when(mapper.fromCreateUpdateDto(any())).thenReturn(order);
        when(orderItemMapper.fromDtoList(any())).thenReturn(List.of(item));
        when(orderRepository.save(any())).thenReturn(order);
        when(mapper.toDto(any())).thenReturn(orderDto);

        OrderWithUserDto result = orderService.createOrder(dto, USER_ID, USER_ROLES);

        assertNotNull(result);
        assertNotNull(result.getOrder());
        assertEquals(1L, result.getOrder().getId());
        assertEquals(USER_ID, result.getUser().getId());
    }

    @Test
    void updateStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.NEW);

        OrderDto orderDto = OrderDto.builder()
                .id(1L)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(mapper.toDto(any())).thenReturn(orderDto);

        OrderWithUserDto result = orderService.updateStatus(
                1L,
                OrderStatus.CANCELLED,
                USER_ID,
                ADMIN_ROLES
        );

        assertNotNull(result.getOrder());
        assertEquals(OrderStatus.CANCELLED, result.getOrder().getStatus());
    }

    @Test
    void getOrderById() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);

        OrderDto orderDto = OrderDto.builder().id(1L).build();

        UserInfoDto user = UserInfoDto.builder()
                .id(USER_ID)
                .active(true)
                .build();

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(mapper.toDto(order)).thenReturn(orderDto);
        when(userServiceClient.getUserById(any(), any(), any())).thenReturn(user);

        OrderWithUserDto result = orderService.getOrderById(1L, USER_ID, USER_ROLES);

        assertNotNull(result.getOrder());
        assertNotNull(result.getUser());
    }


    @Test
    void getOrdersByUser() {
        Order order = new Order();
        order.setId(1L);

        OrderDto orderDto = OrderDto.builder().id(1L).build();

        when(orderRepository.findAllByUserIdAndDeletedFalse(USER_ID))
                .thenReturn(List.of(order));
        when(mapper.toDto(order)).thenReturn(orderDto);

        List<OrderWithUserDto> result = orderService.getOrdersByUser(USER_ID, USER_ROLES);

        assertEquals(1, result.size());
        assertNotNull(result.getFirst().getOrder());
    }


    @Test
    void updateOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setOrderItems(new ArrayList<>());

        Item item = new Item();
        item.setId(1L);

        OrderDto orderDto = OrderDto.builder().id(1L).build();

        OrderCreateUpdateDto dto = new OrderCreateUpdateDto();
        dto.setOrderItems(List.of(new OrderItemDto(1L, 2, 1L, 1L)));

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any())).thenReturn(order);
        when(mapper.toDto(any())).thenReturn(orderDto);
        when(userServiceClient.getUserById(any(), any(), any()))
                .thenReturn(UserInfoDto.builder().id(USER_ID).active(true).build());

        OrderWithUserDto result = orderService.updateOrder(1L, dto, USER_ID, USER_ROLES);

        assertNotNull(result.getOrder());
    }


    @Test
    void deleteOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setDeleted(false);

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(1L, USER_ID, USER_ROLES);

        assertTrue(order.getDeleted());
        verify(orderRepository).save(order);
    }
}