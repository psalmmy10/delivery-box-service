package com.example.deliverybox.service.impl;

import com.example.deliverybox.constant.BoxState;
import com.example.deliverybox.dao.model.Box;
import com.example.deliverybox.dao.model.Item;
import com.example.deliverybox.dao.repository.BoxRepository;
import com.example.deliverybox.dao.repository.ItemRepository;
import com.example.deliverybox.dto.req.CreateBoxRequest;
import com.example.deliverybox.dto.req.ItemRequest;
import com.example.deliverybox.dto.req.LoadItemsRequest;
import com.example.deliverybox.dto.res.BatteryResponse;
import com.example.deliverybox.dto.res.BoxResponse;
import com.example.deliverybox.dto.res.ItemResponse;
import com.example.deliverybox.exception.*;
import com.example.deliverybox.service.BoxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoxServiceImpl implements BoxService{

    /** Minimum battery percentage required before a box may enter LOADING state. */
    private static final int MIN_BATTERY_FOR_LOADING = 25;

    private final BoxRepository boxRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public BoxResponse createBox(CreateBoxRequest request) {
        log.info("Attempting to create box with txref: {}", request.getTxref());
        
        if (boxRepository.existsById(request.getTxref())) {
            log.warn("Box creation failed - duplicate txref: {}", request.getTxref());
            throw new DuplicateBoxException(request.getTxref());
        }
        
        int battery = request.getBatteryCapacity() != null ? request.getBatteryCapacity() : 100;
        log.debug("Box battery capacity set to: {}% (default 100% if not specified)", battery);
        
        // Box box = new Box(request.getTxref(), request.getWeightLimit(), battery, BoxState.IDLE);
        Box box = Box.builder()
                .txref(request.getTxref())
                .weightLimit(request.getWeightLimit())
                .batteryCapacity(battery)
                .state(BoxState.IDLE)
                .build();
                
        Box saved = boxRepository.save(box);
        
        log.info("Box created successfully - txref: {}, weightLimit: {}gr, battery: {}%, state: {}", 
                saved.getTxref(), saved.getWeightLimit(), saved.getBatteryCapacity(), saved.getState());
        
        return BoxResponse.from(saved);
    }

    /**
     * Loads a box with one or more items in a single, atomic operation.
     * - The box must currently be IDLE or already LOADING (allows incremental loading).
     * - The box's battery must be at or above the minimum threshold.
     * - The combined weight of already-loaded items plus the new items must not exceed the box's weight limit.
     * On success the box transitions to LOADED.
     */
    @Transactional
    public BoxResponse loadBox(String txref, LoadItemsRequest request) {
        log.info("Loading box - txref: {}, items count: {}", txref, request.getItems().size());
        log.debug("Items to load: {}", request.getItems());
        
        Box box = getBoxOrThrow(txref);
        log.debug("Current box state: {}, current load: {}gr, weight limit: {}gr, battery: {}%", 
                box.getState(), box.getCurrentLoadWeight(), box.getWeightLimit(), box.getBatteryCapacity());

        if (box.getState() != BoxState.IDLE && box.getState() != BoxState.LOADING) {
            log.warn("Box {} cannot be loaded - invalid state: {}", txref, box.getState());
            throw new InvalidBoxStateException(
                    "Box " + txref + " cannot be loaded while in state " + box.getState());
        }

        if (box.getBatteryCapacity() < MIN_BATTERY_FOR_LOADING) {
            log.warn("Box {} cannot be loaded - low battery: {}% (minimum required: {}%)", 
                    txref, box.getBatteryCapacity(), MIN_BATTERY_FOR_LOADING);
            throw new LowBatteryException(
                    "Box " + txref + " battery is " + box.getBatteryCapacity()
                            + "%, which is below the required " + MIN_BATTERY_FOR_LOADING + "% for loading");
        }

        // Enter LOADING state while items are being added
        box.setState(BoxState.LOADING);
        log.debug("Box {} transitioned to LOADING state", txref);

        int incomingWeight = request.getItems().stream().mapToInt(ItemRequest::getWeight).sum();
        int prospectiveTotal = box.getCurrentLoadWeight() + incomingWeight;
        log.info("Box {} - incoming weight: {}gr, prospective total: {}gr", txref, incomingWeight, prospectiveTotal);

        if (prospectiveTotal > box.getWeightLimit()) {
            log.warn("Box {} weight limit exceeded - current: {}gr, adding: {}gr, limit: {}gr", 
                    txref, box.getCurrentLoadWeight(), incomingWeight, box.getWeightLimit());
            throw new WeightLimitExceededException(
                    "Loading these items (" + incomingWeight + "gr) would bring box " + txref
                            + " to " + prospectiveTotal + "gr, exceeding its weight limit of "
                            + box.getWeightLimit() + "gr");
        }

        for (ItemRequest itemRequest : request.getItems()) {
            Item item = Item.builder()
                    .name(itemRequest.getName())
                    .weight(itemRequest.getWeight())
                    .code(itemRequest.getCode())
                    .box(box)
                    .build();
            box.getItems().add(item);
            log.debug("Item added to box {} - code: {}, name: {}, weight: {}gr", 
                    txref, item.getCode(), item.getName(), item.getWeight());
        }

        // Loading finished successfully
        box.setState(BoxState.LOADED);
        log.info("Box {} successfully loaded - total items: {}, total weight: {}gr", 
                txref, box.getItems().size(), box.getCurrentLoadWeight());

        Box saved = boxRepository.save(box);
        return BoxResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getLoadedItems(String txref) {
        log.info("Retrieving loaded items for box: {}", txref);
        
        getBoxOrThrow(txref);
        
        List<Item> items = itemRepository.findByBoxTxref(txref);
        log.debug("Found {} items for box {}", items.size(), txref);
        
        return items.stream()
                .map(ItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BoxResponse> getAvailableBoxes() {
        log.info("Retrieving available boxes (state: IDLE, battery >= {}%)", MIN_BATTERY_FOR_LOADING);
        
        List<Box> availableBoxes = boxRepository.findByStateAndBatteryCapacityGreaterThanEqual(
                BoxState.IDLE, MIN_BATTERY_FOR_LOADING);
        
        log.info("Found {} available boxes", availableBoxes.size());
        log.debug("Available boxes: {}", availableBoxes.stream().map(Box::getTxref).toList());
        
        return availableBoxes.stream()
                .map(BoxResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatteryResponse getBatteryLevel(String txref) {
        log.info("Retrieving battery level for box: {}", txref);
        BatteryResponse response = BatteryResponse.from(getBoxOrThrow(txref));
        log.debug("Box {} battery capacity: {}%", txref, response.getBatteryCapacity());
        return response;
    }

    @Transactional(readOnly = true)
    public BoxResponse getBox(String txref) {
        log.info("Retrieving box details for: {}", txref);
        
        Box box = getBoxOrThrow(txref);
        log.debug("Retrieved box - state: {}, weight: {}/{}gr, battery: {}%", 
                box.getState(), box.getCurrentLoadWeight(), box.getWeightLimit(), box.getBatteryCapacity());
        
        return BoxResponse.from(box);
    }

    private Box getBoxOrThrow(String txref) {
        log.debug("Looking up box with txref: {}", txref);
        
        return boxRepository.findById(txref)
                .orElseThrow(() -> {
                    log.warn("Box not found with txref: {}", txref);
                    return new BoxNotFoundException(txref);
                });
    }
}