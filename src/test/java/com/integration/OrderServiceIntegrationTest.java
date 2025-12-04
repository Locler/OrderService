package com.integration;

import com.dtos.UserInfoDto;
import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderWithUserDto;
import com.entities.Order;
import com.enums.OrderStatus;
import com.repositories.OrderRep;
import com.services.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ContextConfiguration(initializers = PostgresTestInitializer.class)
class OrderServiceIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("sa")
            .withPassword("sa");

    static WireMockServer wireMockServer = new WireMockServer(8089);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRep orderRep;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        wireMockServer.start();
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
        postgres.stop();
    }

    @BeforeEach
    void setupWireMock() throws Exception {
        wireMockServer.resetAll();
        UserInfoDto user = UserInfoDto.builder()
                .id(1L).email("test@example.com").name("John").surname("Doe").build();

        wireMockServer.stubFor(get(urlPathEqualTo("/by-email"))
                .withQueryParam("email", equalTo("test@example.com"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(user)).withStatus(200)));

        wireMockServer.stubFor(get(urlPathEqualTo("/1"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(user)).withStatus(200)));
    }

    @Test
    void createOrder_success() {
        OrderCreateUpdateDto dto = new OrderCreateUpdateDto();
        dto.setEmail("test@example.com");
        dto.setStatus(OrderStatus.NEW);

        OrderWithUserDto result = orderService.createOrder(dto, "Bearer token");
        assertNotNull(result.getOrder().getId());
        assertEquals(OrderStatus.NEW, result.getOrder().getStatus());
    }

    @Test
    void getOrderById_success() {
        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setUserId(1L);
        order.setDeleted(false);
        order.setTotalPrice(BigDecimal.ZERO);
        order = orderRep.save(order);

        OrderWithUserDto result = orderService.getOrderById(order.getId(), "Bearer token");
        assertEquals(order.getId(), result.getOrder().getId());
    }

    @Test
    void updateOrder_success() {
        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setUserId(1L);
        order.setDeleted(false);
        order.setTotalPrice(BigDecimal.ZERO);
        order = orderRep.save(order);

        OrderCreateUpdateDto dto = new OrderCreateUpdateDto();
        dto.setStatus(OrderStatus.COMPLETED);
        dto.setEmail("test@example.com");

        OrderWithUserDto updated = orderService.updateOrder(order.getId(), dto, "Bearer token");
        assertEquals(OrderStatus.COMPLETED, updated.getOrder().getStatus());
    }

    @Test
    void deleteOrder_success() {
        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setUserId(1L);
        order.setDeleted(false);
        order.setTotalPrice(BigDecimal.ZERO);
        order = orderRep.save(order);

        orderService.deleteOrder(order.getId());
        Order finalOrder = order;
        assertThrows(Exception.class, () -> orderService.getOrderById(finalOrder.getId(), "Bearer token"));

    }

}