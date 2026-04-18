package com.Iot.backend.service;

import com.Iot.backend.dto.DeviceDTO;
import com.Iot.backend.dto.DeviceRequestDTO;
import com.Iot.backend.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DeviceService {

        private final DeviceRepository repository;

        public DeviceService(DeviceRepository repository) {
                this.repository = repository;
        }

        // ===================== DEVICE =====================
        public DeviceDTO createDevice(DeviceRequestDTO request) {

                Map<String, Object> map = new HashMap<>();
                map.put("name", request.getName());
                map.put("location", request.getLocation());

                Map<String, Object> result = repository.createDevice(map);

                return new DeviceDTO(
                                ((Number) result.get("id")).longValue(),
                                (String) result.get("name"),
                                (String) result.get("location"),
                                (Boolean) result.get("status"));
        }

        public List<Map<String, Object>> getAllDevices() {
                return repository.getAllDevices();
        }

        public Map<String, Object> updateDevice(Long id, Map<String, Object> device) {
                return repository.updateDevice(id, device);
        }

        public String deleteDevice(Long id) {
                return repository.deleteDevice(id);
        }

        // ===================== LIMIT DEVICE =====================

        public Map<String, Object> createLimitDevice(Long id, Map<String, Object> device) {
                return repository.createLimitDevice(id, device);
        }

        public List<Map<String, Object>> getAllLimitDevices() {
                return repository.getAllLimitDevices();
        }
}