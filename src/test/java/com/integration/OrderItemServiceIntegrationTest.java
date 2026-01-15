package com.integration;

import com.dtos.request.OrderItemCreateUpdateDto;
import com.dtos.response.OrderItemDto;
import com.entities.Item;
import com.entities.Order;
import com.enums.OrderStatus;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.repositories.ItemRep;
import com.repositories.OrderItemRep;
import com.repositories.OrderRep;
import com.services.OrderItemService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ContextConfiguration(initializers = PostgresTestInitializer.class)
class OrderItemServiceIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("sa")
            .withPassword("sa");

    static WireMockServer wireMockServer = new WireMockServer(8089);

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderRep orderRep;

    @Autowired
    private ItemRep itemRep;

    @Autowired
    private OrderItemRep orderItemRep;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Order savedOrder;
    private Item savedItem;

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
    void setup() throws Exception {
        wireMockServer.resetAll();

        var user = com.dtos.UserInfoDto.builder()
                .id(1L) // важно для userId
                .email("test@example.com")
                .name("John")
                .surname("Doe")
                .build();

        wireMockServer.stubFor(get(urlPathEqualTo("/by-email"))
                .withQueryParam("email", equalTo("test@example.com"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(user))
                        .withStatus(200)));

        wireMockServer.stubFor(get(urlPathEqualTo("/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(user))
                        .withStatus(200)));

        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setUserId(1L);
        order.setDeleted(false);
        order.setTotalPrice(java.math.BigDecimal.ZERO);
        savedOrder = orderRep.save(order);

        Item item = new Item();
        item.setName("Test Item");
        item.setPrice(java.math.BigDecimal.valueOf(100));
        savedItem = itemRep.save(item);
    }

    @Test
    void createOrderItem() {
        OrderItemCreateUpdateDto dto = new OrderItemCreateUpdateDto();
        dto.setOrderId(savedOrder.getId());
        dto.setItemId(savedItem.getId());
        dto.setQuantity(2);

        OrderItemDto result = orderItemService.createOrderItem(dto);

        assertNotNull(result);
        assertEquals(dto.getQuantity(), result.getQuantity());
        assertEquals(savedItem.getId(), result.getItemId());

        Order updatedOrder = orderRep.findById(savedOrder.getId()).orElseThrow();
        assertEquals(java.math.BigDecimal.valueOf(200), updatedOrder.getTotalPrice());
    }

    @Test
    void updateOrderItem() {

        OrderItemCreateUpdateDto dtoCreate = new OrderItemCreateUpdateDto();
        dtoCreate.setOrderId(savedOrder.getId());
        dtoCreate.setItemId(savedItem.getId());
        dtoCreate.setQuantity(2);
        OrderItemDto created = orderItemService.createOrderItem(dtoCreate);


        OrderItemCreateUpdateDto dtoUpdate = new OrderItemCreateUpdateDto();
        dtoUpdate.setOrderId(savedOrder.getId());
        dtoUpdate.setItemId(savedItem.getId());
        dtoUpdate.setQuantity(5);

        OrderItemDto updated = orderItemService.updateOrderItem(created.getId(), dtoUpdate);
        assertEquals(5, updated.getQuantity());

        Order updatedOrder = orderRep.findById(savedOrder.getId()).orElseThrow();
        assertEquals(java.math.BigDecimal.valueOf(500), updatedOrder.getTotalPrice());
    }

    @Test
    void deleteOrderItem() {

        OrderItemCreateUpdateDto dtoCreate = new OrderItemCreateUpdateDto();
        dtoCreate.setOrderId(savedOrder.getId());
        dtoCreate.setItemId(savedItem.getId());
        dtoCreate.setQuantity(3);
        OrderItemDto created = orderItemService.createOrderItem(dtoCreate);

        orderItemService.deleteOrderItem(created.getId());

        assertTrue(orderItemRep.findById(created.getId()).isEmpty());

        Order updatedOrder = orderRep.findById(savedOrder.getId()).orElseThrow();
        assertEquals(java.math.BigDecimal.ZERO, updatedOrder.getTotalPrice());
    }
}
