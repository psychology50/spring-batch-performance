package com.test.batch.repository;

import com.test.batch.dto.DeviceTokenOwner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeviceTokenCustomRepository {
    Page<DeviceTokenOwner> findActivatedDeviceTokenOwners(Pageable pageable);
}
