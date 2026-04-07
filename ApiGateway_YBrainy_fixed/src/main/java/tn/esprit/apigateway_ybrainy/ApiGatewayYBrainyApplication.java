package tn.esprit.apigateway_ybrainy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayYBrainyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayYBrainyApplication.class, args);
    }

}
