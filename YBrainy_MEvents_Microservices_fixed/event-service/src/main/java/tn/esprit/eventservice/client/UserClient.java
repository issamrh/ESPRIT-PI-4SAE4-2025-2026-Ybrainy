package tn.esprit.eventservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tn.esprit.eventservice.dto.UserDto;

import java.util.Optional;

/**
 * Feign client that delegates user lookups to user-service.
 * The service name must match spring.application.name in user-service.
 */
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/User/{id}")
    Optional<UserDto> findById(@PathVariable("id") long id);

    @GetMapping("/User/first-by-role")
    Optional<UserDto> findFirstByRole(@RequestParam("role") String role);

}
