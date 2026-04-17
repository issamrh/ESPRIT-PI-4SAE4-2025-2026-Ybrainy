package tn.esprit.inscriptionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tn.esprit.inscriptionservice.dto.UserDto;

import java.util.List;
import java.util.Optional;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/User/{id}")
    Optional<UserDto> findById(@PathVariable("id") long id);

    @GetMapping("/User/ids-by-role")
    List<Long> findIdsByRole(@RequestParam("role") String role);

    @GetMapping("/User/ids")
    List<Long> findAllIds();
}

//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.PathVariable;
//import tn.esprit.inscriptionservice.dto.UserDto;
//
//import java.util.List;
//import java.util.Optional;
//
///**
// * Feign client that delegates user queries to user-service.
// */
//@FeignClient(name = "user-service")
//public interface UserClient {
//
//    @GetMapping("/User/ids-by-role")
//    List<Long> findIdsByRole(@RequestParam("role") String role);
//
//    @GetMapping("/User/all-ids")
//    List<Long> findAllIds();
//
//    @GetMapping("/User/{id}")
//    Optional<UserDto> findById(@PathVariable("id") long id);
//}
