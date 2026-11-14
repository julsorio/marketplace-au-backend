package com.dev.marketplace.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling habilita la limpieza periódica de RateLimiterService (si se añaden más
// tareas programadas en el futuro, esta es la anotación que ya las habilita a todas).
@SpringBootApplication
@EnableScheduling
public class MarketplaceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketplaceApiApplication.class, args);
	}

}
