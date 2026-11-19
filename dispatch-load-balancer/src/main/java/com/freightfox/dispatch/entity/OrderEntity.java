package com.freightfox.dispatch.entity;

import com.freightfox.dispatch.domain.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
@Entity
@Table(name = "delivery_orders")
public class OrderEntity implements Persistable<String> {

    @Id
    @Column(name = "order_id", length = 64, nullable = false)
    private String orderId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "package_weight", nullable = false, precision = 14, scale = 3)
    private BigDecimal packageWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Transient
    private boolean isNew = true;

    public OrderEntity() {
    }

    public void update(double latitude, double longitude, String address, BigDecimal packageWeight, Priority priority) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.packageWeight = packageWeight;
        this.priority = priority;
    }

    @Override
    public String getId() {
        return orderId;
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

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public BigDecimal getPackageWeight() { return packageWeight; }
    public Priority getPriority() { return priority; }
}
