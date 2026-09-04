package com.example.deliverybox.dto.res;

import com.example.deliverybox.dao.model.Item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemResponse {

    private Long id;
    private String name;
    private Integer weight;
    private String code;

    // public ItemResponse() {
    // }

    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getName(), item.getWeight(), item.getCode());
    }


}
