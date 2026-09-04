package com.example.deliverybox.service;

import java.util.List;

import com.example.deliverybox.dto.req.CreateBoxRequest;
import com.example.deliverybox.dto.req.LoadItemsRequest;
import com.example.deliverybox.dto.res.BatteryResponse;
import com.example.deliverybox.dto.res.BoxResponse;
import com.example.deliverybox.dto.res.ItemResponse;

public interface BoxService {

    public BoxResponse createBox(CreateBoxRequest request);
    public BoxResponse loadBox(String txref, LoadItemsRequest request);
    public List<ItemResponse> getLoadedItems(String txref);
    public List<BoxResponse> getAvailableBoxes();
    public BatteryResponse getBatteryLevel(String txref);
    public BoxResponse getBox(String txref);
    
}
