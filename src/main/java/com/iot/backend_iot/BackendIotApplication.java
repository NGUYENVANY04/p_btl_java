package com.iot.backend_iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendIotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendIotApplication.class, args);
	}

}
