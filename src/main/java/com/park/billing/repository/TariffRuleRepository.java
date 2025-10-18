package com.park.billing.repository;

import com.park.billing.entity.TariffRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TariffRuleRepository extends JpaRepository<TariffRule, Long> {
    Optional<TariffRule> findByLotId(Long lotId);
}
