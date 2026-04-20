package com.Iot.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@CrossOrigin(origins = "*")
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF vì chúng ta dùng API Stateless
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Kích hoạt cấu hình CORS bên dưới
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Không tạo Session (tiết kiệm tài nguyên Server)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Phân quyền: Tạm thời cho phép tất cả để phát triển
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll() // Chỉ rõ các request bắt đầu bằng /api
                        .anyRequest().permitAll())

                // 5. Tắt các trình đăng nhập mặc định của Spring
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 6. Cho phép load Iframe trong cùng domain (quan trọng cho UI của bạn)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Cho phép các nguồn cụ thể (nên thay * bằng domain frontend nếu có thể)
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));

        // Các phương thức HTTP được phép
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Cho phép tất cả các Header (Cần thiết cho Authorization, Content-Type, v.v.)
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Cho phép gửi kèm thông tin xác thực (nếu sau này bạn dùng Cookies)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}