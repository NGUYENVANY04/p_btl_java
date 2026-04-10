package com.example.demo.iot.repository;

import com.example.demo.iot.model.Device;
import com.example.demo.iot.model.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    List<Device> findByStatus(DeviceStatus status);
}

