package com.Iot.backend.service;

import java.util.List;
import java.util.Map;
import com.Iot.backend.repository.*;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRegisterRepository repository;
    private final UserLoginReopository loginrepository;

    public UserService(UserRegisterRepository repository, UserLoginReopository loginrepository) {
        this.repository = repository;
        this.loginrepository = loginrepository;

    }

    public Map<String, Object> registerAccount(Map<String, Object> device) {
        return repository.registerAccount(device);
    }

    public Map<String, Object> login(Map<String, Object> info) {

        String email = (String) info.get("email");
        String password = (String) info.get("password");

        List<Map<String, Object>> users = loginrepository.findByEmail(email);

        if (users == null || users.isEmpty()) {
            throw new RuntimeException("Email không tồn tại");
        }

        Map<String, Object> user = users.get(0);

        if (!user.get("password").equals(password)) {
            throw new RuntimeException("Sai mật khẩu");
        }

        return user;
    }
}
