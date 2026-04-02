package tn.esprit.ybrainy_mevents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class YBrainyMEventsApplication {

    public static void main(String[] args) {
        SpringApplication.run(YBrainyMEventsApplication.class, args);
    }

}
