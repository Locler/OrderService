package com.integration;

import com.checker.AccessChecker;
import com.dtos.UserInfoDto;
import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.request.OrderItemCreateUpdateDto;
import com.dtos.response.OrderItemDto;
import com.dtos.response.OrderWithUserDto;
import com.entities.Item;
import com.repositories.ItemRep;
import com.repositories.OrderItemRep;
import com.repositories.OrderRep;
import com.services.OrderItemService;
import com.services.OrderService;
import com.services.UserServiceClient;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(properties = {
        "spring.profiles.active=test"
})
@WireMockTest(httpPort = 8080)
@ActiveProfiles("test")
public class OrderItemServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ItemRep itemRep;

    @Autowired
    private OrderRep orderRep;

    @Autowired
    private OrderItemRep orderItemRep;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private AccessChecker accessChecker;

    private Long orderId;
    private Long itemId;

    @BeforeAll
    static void setupAll() {
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
    }

    @BeforeEach
    void setup() {
        orderItemRep.deleteAll();
        orderRep.deleteAll();
        itemRep.deleteAll();

        // Мок доступа
        doNothing().when(accessChecker).checkUserAccess(anyLong(), anyLong(), any());
        doNothing().when(accessChecker).checkAdminAccess(any());

        // Создаём Item
        Item item = new Item();
        item.setName("TestItem");
        item.setPrice(BigDecimal.valueOf(100));
        itemRep.save(item);
        itemId = item.getId();

        // Мок активного пользователя
        UserInfoDto mockUser = new UserInfoDto();
        mockUser.setId(1L);
        mockUser.setName("John");
        mockUser.setActive(true);

        when(userServiceClient.getUserById(anyLong(), anyLong(), ArgumentMatchers.<Set<String>>any()))
                .thenReturn(mockUser);

        // Создаём Order через OrderService
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItemId(itemId);
        orderItemDto.setQuantity(2);

        OrderCreateUpdateDto orderDto = new OrderCreateUpdateDto();
        orderDto.setOrderItems(List.of(orderItemDto));

        OrderWithUserDto order = orderService.createOrder(orderDto, 1L, Set.of("ROLE_USER"));
        orderId = order.getOrder().getId();
    }

    @Test
    void createOrderItem() {
        OrderItemCreateUpdateDto dto = new OrderItemCreateUpdateDto();
        dto.setOrderId(orderId);
        dto.setItemId(itemId);
        dto.setQuantity(3);

        OrderItemDto created = orderItemService.createOrderItem(dto, 1L, Set.of("ROLE_USER"));
        assertNotNull(created.getId());
        assertEquals(3, created.getQuantity());
    }

    @Test
    void updateOrderItem() {
        OrderItemCreateUpdateDto dto = new OrderItemCreateUpdateDto();
        dto.setOrderId(orderId);
        dto.setItemId(itemId);
        dto.setQuantity(1);
        OrderItemDto created = orderItemService.createOrderItem(dto, 1L, Set.of("ROLE_USER"));

        dto.setQuantity(5);
        OrderItemDto updated = orderItemService.updateOrderItem(created.getId(), dto, 1L, Set.of("ROLE_USER"));
        assertEquals(5, updated.getQuantity());
    }


    @Test
    void getOrderItemById() {
        OrderItemCreateUpdateDto dto = new OrderItemCreateUpdateDto();
        dto.setOrderId(orderId);
        dto.setItemId(itemId);
        dto.setQuantity(2);
        OrderItemDto created = orderItemService.createOrderItem(dto, 1L, Set.of("ROLE_USER"));

        OrderItemDto fetched = orderItemService.getOrderItemById(created.getId(), 1L, Set.of("ROLE_USER"));
        assertEquals(2, fetched.getQuantity());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public UserServiceClient userServiceClient() {
            return Mockito.mock(UserServiceClient.class);
        }

        @Bean
        public AccessChecker accessChecker() {
            return Mockito.mock(AccessChecker.class);
        }
    }
}