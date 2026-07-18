package com.alahly.momkn.finthos;

import com.alahly.momkn.finthos.integration.config.ProcessorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProcessorProperties.class)
public class FinthosApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinthosApplication.class, args);
	}

}
