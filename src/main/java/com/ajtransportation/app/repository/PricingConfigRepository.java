package com.ajtransportation.app.repository;

import com.ajtransportation.app.model.PricingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingConfigRepository extends JpaRepository<PricingConfig, Integer> {
    // No custom methods needed — use findById(1) to get the single config row
}