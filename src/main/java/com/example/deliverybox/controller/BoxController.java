package com.example.deliverybox.controller;

import com.example.deliverybox.dto.req.CreateBoxRequest;
import com.example.deliverybox.dto.req.LoadItemsRequest;
import com.example.deliverybox.dto.res.BatteryResponse;
import com.example.deliverybox.dto.res.BoxResponse;
import com.example.deliverybox.dto.res.ItemResponse;
import com.example.deliverybox.service.BoxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/boxes")
@RequiredArgsConstructor
@Tag(name = "Delivery Boxes", description = "Endpoints for creating, loading, and monitoring delivery boxes")
public class BoxController {

    private final BoxService boxService;

    @Operation(
            summary = "Create a new box",
            description = "Registers a new delivery box in the system with an IDLE status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Box created successfully",
                    content = @Content(schema = @Schema(implementation = BoxResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public ResponseEntity<BoxResponse> createBox(@Valid @RequestBody CreateBoxRequest request) {
        log.info("Creating new box with request: {}", request);
        BoxResponse response = boxService.createBox(request);
        log.info("Box created successfully with txref: {}", response.getTxref());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get a box by txref",
            description = "Fetches a single delivery box using its unique transaction reference."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Box found",
                    content = @Content(schema = @Schema(implementation = BoxResponse.class))),
            @ApiResponse(responseCode = "404", description = "Box not found")
    })
    @GetMapping("/{txref}")
    public ResponseEntity<BoxResponse> getBox(
            @Parameter(description = "Unique transaction reference of the box", example = "BOX-2024-0001")
            @PathVariable String txref) {
        log.info("Fetching box with txref: {}", txref);
        BoxResponse response = boxService.getBox(txref);
        log.debug("Box fetched: {}", response);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Load items onto a box",
            description = "Adds one or more items to a box identified by txref. Box must be IDLE and have sufficient battery."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items loaded successfully",
                    content = @Content(schema = @Schema(implementation = BoxResponse.class))),
            @ApiResponse(responseCode = "400", description = "Box unavailable or invalid item data"),
            @ApiResponse(responseCode = "404", description = "Box not found")
    })
    @PostMapping("/{txref}/items")
    public ResponseEntity<BoxResponse> loadBox(
            @Parameter(description = "Unique transaction reference of the box", example = "BOX-2024-0001")
            @PathVariable String txref,
            @Valid @RequestBody LoadItemsRequest request) {
        log.info("Loading items onto box [{}], request: {}", txref, request);
        BoxResponse response = boxService.loadBox(txref, request);
        log.info("Box [{}] loaded successfully. Current item count: {}",
                txref, response.getItems() != null ? response.getItems().size() : 0);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get items loaded on a box",
            description = "Returns the list of items currently loaded onto the specified box."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Box not found")
    })
    @GetMapping("/{txref}/items")
    public ResponseEntity<List<ItemResponse>> getLoadedItems(
            @Parameter(description = "Unique transaction reference of the box", example = "BOX-2024-0001")
            @PathVariable String txref) {
        log.info("Fetching loaded items for box: {}", txref);
        List<ItemResponse> items = boxService.getLoadedItems(txref);
        log.debug("Box [{}] has {} loaded item(s)", txref, items.size());
        return ResponseEntity.ok(items);
    }

    @Operation(
            summary = "Get available boxes",
            description = "Returns boxes that are currently IDLE and have battery level >= 25%, i.e. ready for loading."
    )
    @ApiResponse(responseCode = "200", description = "List of available boxes retrieved successfully")
    @GetMapping("/available")
    public ResponseEntity<List<BoxResponse>> getAvailableBoxes() {
        log.info("Fetching available boxes (IDLE, battery >= 25%)");
        List<BoxResponse> boxes = boxService.getAvailableBoxes();
        log.info("Found {} available box(es)", boxes.size());
        return ResponseEntity.ok(boxes);
    }

    @Operation(
            summary = "Get battery level of a box",
            description = "Returns the current battery percentage for the specified box."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Battery level retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BatteryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Box not found")
    })
    @GetMapping("/{txref}/battery")
    public ResponseEntity<BatteryResponse> getBatteryLevel(
            @Parameter(description = "Unique transaction reference of the box", example = "BOX-2024-0001")
            @PathVariable String txref) {
        log.info("Checking battery level for box: {}", txref);
        BatteryResponse response = boxService.getBatteryLevel(txref);
        log.info("Box [{}] battery level: {}%", txref, response.getBatteryCapacity());
        return ResponseEntity.ok(response);
    }
}