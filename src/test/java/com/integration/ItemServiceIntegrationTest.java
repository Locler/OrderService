package com.integration;

import com.dtos.request.ItemCreateUpdateDto;
import com.dtos.response.ItemDto;
import com.repositories.ItemRep;
import com.services.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ContextConfiguration(initializers = PostgresTestInitializer.class)
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;


    @Test
    void createItem_success() {
        ItemCreateUpdateDto dto = new ItemCreateUpdateDto();
        dto.setName("Test Item");
        dto.setPrice(BigDecimal.valueOf(50));
        ItemDto item = itemService.createItem(dto);
        assertNotNull(item.getId());
    }

    @Test
    void getItem_success() {
        ItemDto item = itemService.createItem(new ItemCreateUpdateDto("Test Item", BigDecimal.valueOf(50)));
        ItemDto read = itemService.getItem(item.getId());
        assertEquals("Test Item", read.getName());
    }

    @Test
    void updateItem_success() {
        ItemDto item = itemService.createItem(new ItemCreateUpdateDto("Item 1", BigDecimal.valueOf(30)));
        ItemCreateUpdateDto dto = new ItemCreateUpdateDto("Item 2", BigDecimal.valueOf(60));
        ItemDto updated = itemService.updateItem(item.getId(), dto);
        assertEquals("Item 2", updated.getName());
        assertEquals(BigDecimal.valueOf(60), updated.getPrice());
    }

    @Test
    void deleteItem_success() {
        ItemDto item = itemService.createItem(new ItemCreateUpdateDto("Item X", BigDecimal.valueOf(10)));
        itemService.deleteItem(item.getId());
        assertThrows(RuntimeException.class, () -> itemService.getItem(item.getId()));
    }

}
