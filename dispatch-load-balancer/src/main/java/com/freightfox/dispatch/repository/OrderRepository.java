package com.freightfox.dispatch.repository;

import com.freightfox.dispatch.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
}
