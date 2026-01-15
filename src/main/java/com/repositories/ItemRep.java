package com.repositories;

import com.entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRep extends JpaRepository<Item, Long> {

    Optional<Item> findByName(String name);

}
