package com.freightfox.dispatch.repository;

import com.freightfox.dispatch.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<VehicleEntity, String> {
}
