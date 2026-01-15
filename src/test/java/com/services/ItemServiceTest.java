package com.services;

import com.dtos.request.ItemCreateUpdateDto;
import com.dtos.response.ItemDto;
import com.entities.Item;
import com.mappers.ItemMapper;
import com.repositories.ItemRep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    @Mock
    private ItemRep itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createItem() {
        ItemCreateUpdateDto dto = ItemCreateUpdateDto.builder()
                .name("Phone")
                .price(BigDecimal.valueOf(100))
                .build();

        Item entity = new Item();
        Item saved = new Item();
        ItemDto responseDto = new ItemDto();

        when(itemRepository.findByName("Phone")).thenReturn(Optional.empty());
        when(itemMapper.fromCreateUpdateDto(dto)).thenReturn(entity);
        when(itemRepository.save(entity)).thenReturn(saved);
        when(itemMapper.toDto(saved)).thenReturn(responseDto);

        ItemDto result = itemService.createItem(dto);

        assertThat(result).isEqualTo(responseDto);
        verify(itemRepository).findByName("Phone");
        verify(itemRepository).save(entity);
    }

    @Test
    void createItemDuplicateName() {
        ItemCreateUpdateDto dto = ItemCreateUpdateDto.builder()
                .name("Phone")
                .price(BigDecimal.valueOf(100))
                .build();

        when(itemRepository.findByName("Phone")).thenReturn(Optional.of(new Item()));

        assertThatThrownBy(() -> itemService.createItem(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exists");
        verify(itemRepository).findByName("Phone");
        verify(itemRepository, never()).save(any());
    }

    @Test
    void getItem() {
        Item item = new Item();
        ItemDto dto = new ItemDto();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(dto);

        ItemDto result = itemService.getItem(1L);
        assertThat(result).isEqualTo(dto);
    }

    @Test
    void getItemNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> itemService.getItem(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getAllItems() {
        Item i1 = new Item(); i1.setName("a");
        Item i2 = new Item(); i2.setName("b");
        ItemDto d1 = new ItemDto();
        ItemDto d2 = new ItemDto();

        Page<Item> page = new PageImpl<>(List.of(i1, i2));
        Pageable pageable = PageRequest.of(0, 10);

        when(itemRepository.findAll(pageable)).thenReturn(page);
        when(itemMapper.toDto(i1)).thenReturn(d1);
        when(itemMapper.toDto(i2)).thenReturn(d2);

        Page<ItemDto> res = itemService.getAllItems(pageable);

        assertThat(res.getContent()).containsExactly(d1, d2);
    }

    @Test
    void updateItemUniqueNameCheck() {
        Long id = 5L;
        Item existing = new Item();
        existing.setId(id);
        ItemCreateUpdateDto dto = ItemCreateUpdateDto.builder()
                .name("SameName")
                .price(BigDecimal.valueOf(10))
                .build();

        Item found = new Item();
        found.setId(id); // same id -> should not throw

        when(itemRepository.findById(id)).thenReturn(Optional.of(existing));
        when(itemRepository.findByName("SameName")).thenReturn(Optional.of(found));
        // mapper updateFromDto called
        doNothing().when(itemMapper).updateFromDto(dto, existing);
        when(itemRepository.save(existing)).thenReturn(existing);
        when(itemMapper.toDto(existing)).thenReturn(new ItemDto());

        ItemDto res = itemService.updateItem(id, dto);
        assertThat(res).isNotNull();
        verify(itemRepository).findByName("SameName");
    }

    @Test
    void updateItemUniqueNameConflict() {
        Long id = 5L;
        Item existing = new Item();
        existing.setId(id);
        ItemCreateUpdateDto dto = ItemCreateUpdateDto.builder()
                .name("OtherName")
                .price(BigDecimal.valueOf(10))
                .build();

        Item other = new Item();
        other.setId(999L); // different id -> conflict

        when(itemRepository.findById(id)).thenReturn(Optional.of(existing));
        when(itemRepository.findByName("OtherName")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> itemService.updateItem(id, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exists");
    }

    @Test
    void deleteItem() {
        Item item = new Item();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        doNothing().when(itemRepository).delete(item);

        itemService.deleteItem(1L);

        verify(itemRepository).delete(item);
    }

    @Test
    void deleteItem_notFound() {
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> itemService.deleteItem(10L))
                .isInstanceOf(RuntimeException.class);
    }
}
