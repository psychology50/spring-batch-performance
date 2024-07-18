package com.test.batch.repository;

import com.test.batch.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long>, DeviceTokenCustomRepository {
}
