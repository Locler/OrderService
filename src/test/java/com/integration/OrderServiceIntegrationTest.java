package com.integration;

import com.checker.AccessChecker;
import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderWithUserDto;
import com.dtos.response.OrderItemDto;
import com.dtos.UserInfoDto;
import com.entities.Item;
import com.repositories.ItemRep;
import com.repositories.OrderRep;
import com.services.OrderService;
import com.services.UserServiceClient;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.kafka.listener.auto-startup=false"
})
@WireMockTest(httpPort = 8080)
@ActiveProfiles("test")
public class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void setupAll() {
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private ItemRep itemRep;

    @Autowired
    private OrderRep orderRep;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private AccessChecker accessChecker;

    @BeforeEach
    void setup() {
        orderRep.deleteAll();
        itemRep.deleteAll();

        // Мок проверки доступа (void методы)
        doNothing().when(accessChecker).checkUserAccess(anyLong(), anyLong(), any());
        doNothing().when(accessChecker).checkAdminAccess(any());

        // Мок активного пользователя
        UserInfoDto mockUser = new UserInfoDto();
        mockUser.setId(1L);
        mockUser.setName("John");
        mockUser.setActive(true);

        when(userServiceClient.getUserById(anyLong(), anyLong(), anySet()))
                .thenReturn(mockUser);

        // Созд тестовый Item
        Item item = new Item();
        item.setName("Test Item");
        item.setPrice(BigDecimal.valueOf(100));
        itemRep.save(item);
    }

    @Test
    void createOrder() {
        Item savedItem = itemRep.findAll().getFirst();

        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItemId(savedItem.getId());
        orderItemDto.setQuantity(2);

        OrderCreateUpdateDto orderDto = new OrderCreateUpdateDto();
        orderDto.setOrderItems(List.of(orderItemDto));

        OrderWithUserDto createdOrder = orderService.createOrder(orderDto, 1L, Set.of("ROLE_USER"));

        assertNotNull(createdOrder);
        assertEquals("John", createdOrder.getUser().getName());
        assertEquals(1, createdOrder.getOrder().getOrderItems().size());

        assertEquals(BigDecimal.valueOf(200).setScale(2), createdOrder.getOrder().getTotalPrice());
    }

    @Test
    void getOrderById() {

        OrderWithUserDto created = createSampleOrder();

        OrderWithUserDto fetched = orderService.getOrderById(created.getOrder().getId(), 1L, Set.of("ROLE_USER"));

        assertNotNull(fetched);
        assertEquals(created.getOrder().getId(), fetched.getOrder().getId());
        assertEquals(BigDecimal.valueOf(200).setScale(2), fetched.getOrder().getTotalPrice());
    }

    @Test
    void updateOrder() {
        OrderWithUserDto created = createSampleOrder();


        OrderItemDto updateItem = new OrderItemDto();
        updateItem.setItemId(itemRep.findAll().getFirst().getId());
        updateItem.setQuantity(3);

        OrderCreateUpdateDto updateDto = new OrderCreateUpdateDto();
        updateDto.setOrderItems(List.of(updateItem));

        OrderWithUserDto updated = orderService.updateOrder(created.getOrder().getId(), updateDto, 1L, Set.of("ROLE_USER"));

        assertEquals(BigDecimal.valueOf(300).setScale(2), updated.getOrder().getTotalPrice());
        assertEquals(1, updated.getOrder().getOrderItems().size());
    }

    @Test
    void getUserOrders() {
        createSampleOrder();

        List<OrderWithUserDto> orders = orderService.getOrdersByUser(1L, Set.of("ROLE_USER"));
        assertFalse(orders.isEmpty());
        assertEquals(BigDecimal.valueOf(200).setScale(2), orders.getFirst().getOrder().getTotalPrice());
    }

    private OrderWithUserDto createSampleOrder() {
        Item savedItem = itemRep.findAll().getFirst();

        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItemId(savedItem.getId());
        orderItemDto.setQuantity(2);

        OrderCreateUpdateDto orderDto = new OrderCreateUpdateDto();
        orderDto.setOrderItems(List.of(orderItemDto));

        return orderService.createOrder(orderDto, 1L, Set.of("ROLE_USER"));
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