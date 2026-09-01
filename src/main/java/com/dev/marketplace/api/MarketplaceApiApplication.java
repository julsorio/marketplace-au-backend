package com.dev.marketplace.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación Spring Boot del backend de marketplace-au.
 */
@SpringBootApplication
public class MarketplaceApiApplication {

	/**
	 * Arranca el contexto de Spring Boot y el servidor embebido de la API.
	 *
	 * @param args argumentos de línea de comandos pasados a la aplicación
	 */
	public static void main(String[] args) {
		SpringApplication.run(MarketplaceApiApplication.class, args);
	}

}
