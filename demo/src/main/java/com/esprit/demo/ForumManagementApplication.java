package com.esprit.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ForumManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForumManagementApplication.class, args);
	}

}
