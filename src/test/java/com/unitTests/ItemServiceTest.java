package com.unitTests;

import com.checker.AccessChecker;
import com.dtos.request.ItemCreateUpdateDto;
import com.dtos.response.ItemDto;
import com.entities.Item;
import com.mappers.ItemMapper;
import com.repositories.ItemRep;
import com.services.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    @Mock
    private ItemRep itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private AccessChecker accessChecker;

    @InjectMocks
    private ItemService itemService;

    private Item item;
    private ItemDto itemDto;
    private ItemCreateUpdateDto createDto;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        item = new Item();
        item.setId(1L);
        item.setName("TestItem");
        item.setPrice(new BigDecimal("10.0"));

        itemDto = ItemDto.builder()
                .id(1L)
                .name("TestItem")
                .price(new BigDecimal("10.0"))
                .build();

        createDto = ItemCreateUpdateDto.builder()
                .name("TestItem")
                .price(new BigDecimal("10.0"))
                .build();
    }

    @Test
    void getItem() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        ItemDto result = itemService.getItem(1L, 1L, Set.of("ROLE_USER"));
        assertEquals("TestItem", result.getName());
        verify(accessChecker).checkUserAccess(1L, 1L, Set.of("ROLE_USER"));
    }

    @Test
    void createItem() {
        when(itemRepository.findByName("TestItem")).thenReturn(Optional.empty());
        when(itemMapper.fromCreateUpdateDto(createDto)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        ItemDto result = itemService.createItem(createDto, Set.of("ROLE_ADMIN"));
        assertEquals("TestItem", result.getName());
        verify(accessChecker).checkAdminAccess(Set.of("ROLE_ADMIN"));
    }

    @Test
    void createItemDuplicateName() {
        when(itemRepository.findByName("TestItem")).thenReturn(Optional.of(item));
        assertThrows(IllegalStateException.class, () ->
                itemService.createItem(createDto, Set.of("ROLE_ADMIN")));
    }

    @Test
    void updateItem() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.findByName("TestItem")).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        ItemDto result = itemService.updateItem(1L, createDto, Set.of("ROLE_ADMIN"));
        assertEquals("TestItem", result.getName());
    }

    @Test
    void deleteItem() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        itemService.deleteItem(1L, Set.of("ROLE_ADMIN"));
        verify(itemRepository).delete(item);
    }

    @Test
    void getAllItems() {
        Pageable pageable = PageRequest.of(0, 10);
        when(itemRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(item)));
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        Page<ItemDto> page = itemService.getAllItems(pageable, 1L, Set.of("ROLE_USER"));
        assertEquals(1, page.getTotalElements());
    }
}
