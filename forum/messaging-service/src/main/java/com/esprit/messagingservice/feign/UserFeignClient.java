package com.esprit.messagingservice.feign;

import com.esprit.messagingservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/api/users/{userId}")
    UserDto getUser(@PathVariable Long userId);

    @GetMapping("/api/users")
    List<UserDto> getAllUsers();
}
