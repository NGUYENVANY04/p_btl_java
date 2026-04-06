package com.iot.backend_iot.repository;

import com.iot.backend_iot.model.Device;
import com.iot.backend_iot.model.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    List<Device> findByStatus(DeviceStatus status);
}

