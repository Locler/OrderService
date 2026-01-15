package com.repositories;

import com.entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRep extends JpaRepository<Item, Long> {
}
