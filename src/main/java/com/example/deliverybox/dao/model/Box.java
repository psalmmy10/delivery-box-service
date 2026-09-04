package com.example.deliverybox.dao.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import com.example.deliverybox.constant.BoxState;

@Entity
@Table(name = "boxes")
@Data
@AllArgsConstructor
@Builder
public class Box {

    @Id
    @Column(name = "txref", length = 20, nullable = false, updatable = false)
    private String txref;

    @Column(name = "weight_limit", nullable = false)
    private Integer weightLimit;

    @Column(name = "battery_capacity", nullable = false)
    private Integer batteryCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private BoxState state;

    @OneToMany(mappedBy = "box", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Item> items = new ArrayList<>();

    public Box() {
    }

    public Box(String txref, Integer weightLimit, Integer batteryCapacity, BoxState state) {
        this.txref = txref;
        this.weightLimit = weightLimit;
        this.batteryCapacity = batteryCapacity;
        this.state = state;
        this.items = new ArrayList<>(); 
    }

    @Transient
    public int getCurrentLoadWeight() {
        return items.stream().mapToInt(Item::getWeight).sum();
    }
}
