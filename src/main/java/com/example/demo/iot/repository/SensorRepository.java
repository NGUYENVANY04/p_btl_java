package com.example.demo.iot.repository;

import com.example.demo.iot.model.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SensorRepository extends JpaRepository<SensorData, Long> {
    SensorData findTopByOrderByTimestampDesc();

    List<SensorData> findByTimestampBetweenOrderByTimestampAsc(LocalDateTime from, LocalDateTime to);

    List<SensorData> findByDeviceIdOrderByTimestampAsc(String deviceId);

    List<SensorData> findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(String deviceId, LocalDateTime from, LocalDateTime to);

    List<SensorData> findByDeviceIdAndTimestampGreaterThanEqualOrderByTimestampAsc(String deviceId, LocalDateTime from);

    List<SensorData> findByDeviceIdAndTimestampLessThanEqualOrderByTimestampAsc(String deviceId, LocalDateTime to);

    List<SensorData> findByTimestampGreaterThanEqualOrderByTimestampAsc(LocalDateTime from);

    List<SensorData> findByTimestampLessThanEqualOrderByTimestampAsc(LocalDateTime to);
}

