package com.iot.backend_iot.repository;

import com.iot.backend_iot.model.Alert;
import com.iot.backend_iot.model.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    boolean existsByDeviceIdAndTypeAndMetricAndResolvedFalse(String deviceId, AlertType type, String metric);

    boolean existsByDeviceIdAndTypeAndResolvedFalseAndMetricIsNull(String deviceId, AlertType type);

    List<Alert> findByOrderByCreatedAtDesc();

    List<Alert> findByResolvedOrderByCreatedAtDesc(boolean resolved);

    List<Alert> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    List<Alert> findByDeviceIdAndResolvedOrderByCreatedAtDesc(String deviceId, boolean resolved);

    List<Alert> findByTypeOrderByCreatedAtDesc(AlertType type);

    List<Alert> findByTypeAndResolvedOrderByCreatedAtDesc(AlertType type, boolean resolved);

    List<Alert> findByDeviceIdAndTypeOrderByCreatedAtDesc(String deviceId, AlertType type);

    List<Alert> findByDeviceIdAndTypeAndResolvedOrderByCreatedAtDesc(String deviceId, AlertType type, boolean resolved);
}

