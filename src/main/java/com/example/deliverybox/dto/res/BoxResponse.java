package com.example.deliverybox.dto.res;

import java.util.List;
import java.util.stream.Collectors;
import com.example.deliverybox.dao.model.Box;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BoxResponse {

    private String txref;
    private Integer weightLimit;
    private Integer batteryCapacity;
    private String state;
    private Integer currentLoadWeight;
    private Integer availableCapacity;
    private List<ItemResponse> items;

    public static BoxResponse from(Box box) {
        int currentLoad = box.getCurrentLoadWeight();
        List<ItemResponse> itemResponses = box.getItems() == null
                ? List.of()
                : box.getItems().stream()
                    .map(ItemResponse::from) 
                    .collect(Collectors.toList());

        return new BoxResponse(
                box.getTxref(),
                box.getWeightLimit(),
                box.getBatteryCapacity(),
                box.getState().name(),
                currentLoad,
                box.getWeightLimit() - currentLoad,
                itemResponses
        );
    }
}