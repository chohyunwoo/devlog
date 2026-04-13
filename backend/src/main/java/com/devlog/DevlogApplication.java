package com.devlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DevlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevlogApplication.class, args);
	}

}
