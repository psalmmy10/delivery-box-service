package com.example.deliverybox.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.deliverybox.dao.model.Item;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByBoxTxref(String txref);
}
