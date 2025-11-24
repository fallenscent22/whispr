package com.nikhitha.whispr;

// import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class WhisprApplication {

	// @PostConstruct
	// public void init() {
	// 	TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
	// }

	public static void main(String[] args) {
		SpringApplication.run(WhisprApplication.class, args);
	}
}
