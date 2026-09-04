package com.example.deliverybox.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatteryResponse {

    private String txref;
    private Integer batteryCapacity;

    public static BatteryResponse from(com.example.deliverybox.dao.model.Box box) {
        return new BatteryResponse(box.getTxref(), box.getBatteryCapacity());
    }
}