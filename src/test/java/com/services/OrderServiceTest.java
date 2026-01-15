package com.services;

import com.dtos.UserInfoDto;
import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderDto;
import com.dtos.response.OrderWithUserDto;
import com.entities.Order;
import com.enums.OrderStatus;
import com.mappers.OrderMapper;
import com.repositories.OrderRep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock
    private OrderRep orderRepository;

    @Mock
    private OrderMapper mapper;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createOrder_success_setsUserIdAndReturns() {

        // given
        OrderCreateUpdateDto dto = new OrderCreateUpdateDto();
        dto.setStatus(OrderStatus.NEW);
        dto.setEmail("test@mail.com");

        UserInfoDto user = UserInfoDto.builder()
                .id(10L)
                .name("John")
                .surname("Doe")
                .email("test@mail.com")
                .active(true)
                .build();

        Order orderEntityBeforeSave = new Order();
        orderEntityBeforeSave.setStatus(OrderStatus.NEW);

        Order orderEntitySaved = new Order();
        orderEntitySaved.setId(100L);
        orderEntitySaved.setUserId(10L);
        orderEntitySaved.setStatus(OrderStatus.NEW);
        orderEntitySaved.setTotalPrice(BigDecimal.ZERO);

        OrderDto orderDto = new OrderDto();
        orderDto.setId(100L);
        orderDto.setStatus(OrderStatus.NEW);

        // mocks
        when(userServiceClient.getUserByEmail(anyString(), anyString()))
                .thenReturn(user);

        when(mapper.fromCreateUpdateDto(dto))
                .thenReturn(orderEntityBeforeSave);

        when(orderRepository.save(orderEntityBeforeSave))
                .thenReturn(orderEntitySaved);

        when(mapper.toDto(orderEntitySaved))
                .thenReturn(orderDto);

        // when
        OrderWithUserDto result = service.createOrder(dto, "token");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrder()).isNotNull();
        assertThat(result.getOrder().getId()).isEqualTo(100L);
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(result.getUser().getId()).isEqualTo(10L);
        assertThat(result.getUser().getEmail()).isEqualTo("test@mail.com");
    }

    @Test
    void createOrder_invalidDto_throws() {
        assertThatThrownBy(() -> service.createOrder(null, "auth"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getOrderById_success_callsUserClient() {
        Order order = new Order();
        order.setId(5L);
        order.setUserId(11L);

        when(orderRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(order));
        when(userServiceClient.getUserById(11L, "auth")).thenReturn(UserInfoDto.builder().email("a").build());
        when(mapper.toDto(order)).thenReturn(null);

        OrderWithUserDto dto = service.getOrderById(5L, "auth");
        assertThat(dto).isNotNull();
    }

    @Test
    void getAllOrders_filtersAndPages() {
        // given
        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .status(OrderStatus.NEW)
                .totalPrice(BigDecimal.ZERO)
                .deleted(false)
                .build();

        Page<Order> mockPage = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // user mock
        UserInfoDto mockUser = UserInfoDto.builder()
                .id(10L)
                .email("test@mail.com")
                .name("John")
                .surname("Doe")
                .active(true)
                .build();

        when(userServiceClient.getUserById(eq(10L), any()))
                .thenReturn(mockUser);

        // mapper mock
        OrderDto orderDto = OrderDto.builder()
                .id(1L)
                .status(OrderStatus.NEW)
                .totalPrice(BigDecimal.ZERO)
                .deleted(false)
                .orderItems(List.of())
                .build();

        when(mapper.toDto(any(Order.class))).thenReturn(orderDto);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<OrderWithUserDto> result = service.getAllOrders(
                List.of(OrderStatus.NEW),
                null, null,
                pageable,
                "token"
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);

        OrderWithUserDto dto = result.getContent().getFirst();

        assertThat(dto.getOrder()).isNotNull();
        assertThat(dto.getOrder().getId()).isEqualTo(1L);
        assertThat(dto.getUser().getEmail()).isEqualTo("test@mail.com");
    }

    @Test
    void updateOrder_success() {
        Order existing = new Order();
        existing.setId(3L);
        existing.setUserId(7L);
        when(orderRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(existing));

        OrderCreateUpdateDto dto = new OrderCreateUpdateDto();
        dto.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.save(existing)).thenReturn(existing);
        when(userServiceClient.getUserById(7L, "auth")).thenReturn(UserInfoDto.builder().email("a").build());
        when(mapper.toDto(existing)).thenReturn(null);

        OrderWithUserDto res = service.updateOrder(3L, dto, "auth");
        assertThat(res).isNotNull();
    }

    @Test
    void deleteOrder_softDeletes() {
        Order existing = new Order();
        existing.setId(4L);
        when(orderRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(existing);

        service.deleteOrder(4L);
        assertThat(existing.getDeleted()).isTrue();
    }
}
