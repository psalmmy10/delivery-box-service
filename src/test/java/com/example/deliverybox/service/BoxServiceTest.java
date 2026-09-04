package com.example.deliverybox.service;

import com.example.deliverybox.constant.BoxState;
import com.example.deliverybox.dao.model.Box;
import com.example.deliverybox.dao.repository.BoxRepository;
import com.example.deliverybox.dao.repository.ItemRepository;
import com.example.deliverybox.dto.*;
import com.example.deliverybox.dto.req.CreateBoxRequest;
import com.example.deliverybox.dto.req.ItemRequest;
import com.example.deliverybox.dto.req.LoadItemsRequest;
import com.example.deliverybox.dto.res.BoxResponse;
import com.example.deliverybox.exception.*;
import com.example.deliverybox.service.impl.BoxServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoxServiceTest {

    @Mock
    private BoxRepository boxRepository;

    @Mock
    private ItemRepository itemRepository;

    private BoxService boxService;

    @BeforeEach
    void setUp() {
        boxService = new BoxServiceImpl(boxRepository, itemRepository);
    }

    @Test
    void createBox_succeeds_whenTxrefIsUnique() {
        CreateBoxRequest request = new CreateBoxRequest();
        request.setTxref("BOX-100");
        request.setWeightLimit(500);
        request.setBatteryCapacity(100);

        when(boxRepository.existsById("BOX-100")).thenReturn(false);
        when(boxRepository.save(any(Box.class))).thenAnswer(inv -> inv.getArgument(0));

        BoxResponse response = boxService.createBox(request);

        assertThat(response.getTxref()).isEqualTo("BOX-100");
        assertThat(response.getState()).isEqualTo("IDLE");
        assertThat(response.getBatteryCapacity()).isEqualTo(100);
    }

    @Test
    void createBox_throws_whenTxrefAlreadyExists() {
        CreateBoxRequest request = new CreateBoxRequest();
        request.setTxref("BOX-100");
        request.setWeightLimit(500);

        when(boxRepository.existsById("BOX-100")).thenReturn(true);

        assertThatThrownBy(() -> boxService.createBox(request))
                .isInstanceOf(DuplicateBoxException.class);
    }

    @Test
    void loadBox_throws_whenWeightExceedsLimit() {
        Box box = new Box("BOX-100", 500, 100, BoxState.IDLE);
        when(boxRepository.findById("BOX-100")).thenReturn(Optional.of(box));

        ItemRequest heavyItem = new ItemRequest();
        heavyItem.setName("Big-Item");
        heavyItem.setWeight(600);
        heavyItem.setCode("BIG_ITEM_1");

        LoadItemsRequest request = new LoadItemsRequest();
        request.setItems(List.of(heavyItem));

        assertThatThrownBy(() -> boxService.loadBox("BOX-100", request))
                .isInstanceOf(WeightLimitExceededException.class);
    }

    @Test
    void loadBox_throws_whenBatteryBelowThreshold() {
        Box box = new Box("BOX-100", 500, 10, BoxState.IDLE);
        when(boxRepository.findById("BOX-100")).thenReturn(Optional.of(box));

        ItemRequest item = new ItemRequest();
        item.setName("Small-Item");
        item.setWeight(50);
        item.setCode("SMALL_ITEM_1");

        LoadItemsRequest request = new LoadItemsRequest();
        request.setItems(List.of(item));

        assertThatThrownBy(() -> boxService.loadBox("BOX-100", request))
                .isInstanceOf(LowBatteryException.class);
    }

    @Test
    void loadBox_succeeds_andTransitionsToLoaded_whenWithinLimits() {
        Box box = new Box("BOX-100", 500, 100, BoxState.IDLE);
        when(boxRepository.findById("BOX-100")).thenReturn(Optional.of(box));
        when(boxRepository.save(any(Box.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemRequest item = new ItemRequest();
        item.setName("Small-Item");
        item.setWeight(100);
        item.setCode("SMALL_ITEM_1");

        LoadItemsRequest request = new LoadItemsRequest();
        request.setItems(List.of(item));

        BoxResponse response = boxService.loadBox("BOX-100", request);

        assertThat(response.getState()).isEqualTo("LOADED");
        assertThat(response.getCurrentLoadWeight()).isEqualTo(100);
    }

    @Test
    void loadBox_throws_whenBoxNotFound() {
        when(boxRepository.findById(anyString())).thenReturn(Optional.empty());

        LoadItemsRequest request = new LoadItemsRequest();
        ItemRequest item = new ItemRequest();
        item.setName("X");
        item.setWeight(10);
        item.setCode("X1");
        request.setItems(List.of(item));

        assertThatThrownBy(() -> boxService.loadBox("MISSING", request))
                .isInstanceOf(BoxNotFoundException.class);
    }

    @Test
    void loadBox_throws_whenBoxNotInLoadableState() {
        Box box = new Box("BOX-100", 500, 100, BoxState.DELIVERING);
        when(boxRepository.findById("BOX-100")).thenReturn(Optional.of(box));

        ItemRequest item = new ItemRequest();
        item.setName("X");
        item.setWeight(10);
        item.setCode("X1");
        LoadItemsRequest request = new LoadItemsRequest();
        request.setItems(List.of(item));

        assertThatThrownBy(() -> boxService.loadBox("BOX-100", request))
                .isInstanceOf(InvalidBoxStateException.class);
    }

    @Test
    void getAvailableBoxes_returnsOnlyIdleBoxesAboveBatteryThreshold() {
        Box available = new Box("BOX-100", 500, 100, BoxState.IDLE);
        when(boxRepository.findByStateAndBatteryCapacityGreaterThanEqual(BoxState.IDLE, 25))
                .thenReturn(List.of(available));

        List<BoxResponse> result = boxService.getAvailableBoxes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTxref()).isEqualTo("BOX-100");
    }
}
