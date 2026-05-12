package com.antojito.maps_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MapsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MapsBackendApplication.class, args);
	}

}