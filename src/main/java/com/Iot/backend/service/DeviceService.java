package com.Iot.backend.service;

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
        public Map<String, Object> createDevice(Map<String, Object> device) {
                return repository.createDevice(device);
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