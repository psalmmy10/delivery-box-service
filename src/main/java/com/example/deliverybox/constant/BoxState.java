package com.example.deliverybox.constant;

/**
 * Lifecycle states of a delivery box.
 *
 * IDLE        -> box is empty, parked, and available for loading
 * LOADING     -> box is currently being loaded with items (transient/in-progress state)
 * LOADED      -> box has items and is ready to be dispatched
 * DELIVERING  -> box is out delivering its items
 * DELIVERED   -> box has delivered its items
 * RETURNING   -> box is heading back to base after delivering
 */
public enum BoxState {
    IDLE,
    LOADING,
    LOADED,
    DELIVERING,
    DELIVERED,
    RETURNING
}
