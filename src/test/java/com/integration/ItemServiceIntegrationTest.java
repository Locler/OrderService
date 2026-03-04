package com.integration;

import com.checker.AccessChecker;
import com.dtos.request.ItemCreateUpdateDto;
import com.dtos.response.ItemDto;
import com.entities.Item;
import com.repositories.ItemRep;
import com.services.ItemService;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;

@Testcontainers
@SpringBootTest(properties = {
        "spring.profiles.active=test"
})
@WireMockTest(httpPort = 8080)
@ActiveProfiles("test")
public class ItemServiceIntegrationTest {

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
    private ItemService itemService;

    @Autowired
    private ItemRep itemRep;

    @Autowired
    private AccessChecker accessChecker;

    @BeforeEach
    void setup() {
        itemRep.deleteAll();
        // Мок доступа ADMIN
        doNothing().when(accessChecker).checkAdminAccess(any());
    }

    @Test
    void createItem() {
        ItemCreateUpdateDto dto = new ItemCreateUpdateDto();
        dto.setName("NewItem");
        dto.setPrice(BigDecimal.valueOf(150));

        ItemDto created = itemService.createItem(dto, Set.of("ROLE_ADMIN"));
        assertNotNull(created.getId());
        assertEquals("NewItem", created.getName());
        assertEquals(BigDecimal.valueOf(150), created.getPrice());
    }

    @Test
    void updateItem() {
        Item item = new Item();
        item.setName("OldItem");
        item.setPrice(BigDecimal.valueOf(100));
        itemRep.save(item);

        ItemCreateUpdateDto updateDto = new ItemCreateUpdateDto();
        updateDto.setName("UpdatedItem");
        updateDto.setPrice(BigDecimal.valueOf(200));

        ItemDto updated = itemService.updateItem(item.getId(), updateDto, Set.of("ROLE_ADMIN"));
        assertEquals("UpdatedItem", updated.getName());
        assertEquals(BigDecimal.valueOf(200), updated.getPrice());
    }

    @Test
    void getItem() {
        Item item = new Item();
        item.setName("Sample");
        item.setPrice(BigDecimal.valueOf(120));
        itemRep.save(item);

        // Мок доступ USER
        doNothing().when(accessChecker).checkUserAccess(anyLong(), anyLong(), any());

        ItemDto fetched = itemService.getItem(item.getId(), 1L, Set.of("ROLE_USER"));
        assertEquals("Sample", fetched.getName());
        assertEquals(BigDecimal.valueOf(120).setScale(2), fetched.getPrice());
    }

    @Test
    void deleteItem() {
        Item item = new Item();
        item.setName("ToDelete");
        item.setPrice(BigDecimal.valueOf(100));
        itemRep.save(item);

        itemService.deleteItem(item.getId(), Set.of("ROLE_ADMIN"));
        assertTrue(itemRep.findById(item.getId()).isEmpty());
    }

    @Test
    void getItems() {
        Item item1 = new Item();
        item1.setName("Item1");
        item1.setPrice(BigDecimal.valueOf(50));
        Item item2 = new Item();
        item2.setName("Item2");
        item2.setPrice(BigDecimal.valueOf(100));
        itemRep.saveAll(List.of(item1, item2));

        doNothing().when(accessChecker).checkUserAccess(anyLong(), anyLong(), any());

        var items = itemService.getAllItems(PageRequest.of(0, 10), 1L, Set.of("ROLE_USER"));
        assertEquals(2, items.getContent().size());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AccessChecker accessChecker() {
            return Mockito.mock(AccessChecker.class);
        }
    }
}