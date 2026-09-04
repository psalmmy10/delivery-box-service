package com.example.deliverybox.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.deliverybox.constant.BoxState;
import com.example.deliverybox.dao.model.Box;

import java.util.List;

public interface BoxRepository extends JpaRepository<Box, String> {

    List<Box> findByStateAndBatteryCapacityGreaterThanEqual(BoxState state, Integer batteryCapacity);
}
