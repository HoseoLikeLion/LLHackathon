package com.hackathon.skinroutine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // config/AppProperties(app.*) 바인딩 활성화
public class SkinRoutineApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkinRoutineApplication.class, args);
	}

}
