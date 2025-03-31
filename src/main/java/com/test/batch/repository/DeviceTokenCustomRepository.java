package com.test.batch.repository;

import com.test.batch.dto.DeviceTokenOwner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface DeviceTokenCustomRepository {
    Slice<DeviceTokenOwner> findActivatedDeviceTokenOwners(Pageable pageable);
}
