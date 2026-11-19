package com.freightfox.dispatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;


@Entity
@Table(name = "vehicles")
public class VehicleEntity implements Persistable<String> {

    @Id
    @Column(name = "vehicle_id", length = 64, nullable = false)
    private String vehicleId;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal capacity;

    @Column(name = "current_latitude", nullable = false)
    private double currentLatitude;

    @Column(name = "current_longitude", nullable = false)
    private double currentLongitude;

    @Column(name = "current_address", nullable = false, length = 500)
    private String currentAddress;

    @Transient
    private boolean isNew = true;

    public VehicleEntity() {
    }

    public void update(BigDecimal capacity, double currentLatitude, double currentLongitude, String currentAddress) {
        this.capacity = capacity;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.currentAddress = currentAddress;
    }

    @Override
    public String getId() {
        return vehicleId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public BigDecimal getCapacity() { return capacity; }
    public double getCurrentLatitude() { return currentLatitude; }
    public double getCurrentLongitude() { return currentLongitude; }
    public String getCurrentAddress() { return currentAddress; }
}
