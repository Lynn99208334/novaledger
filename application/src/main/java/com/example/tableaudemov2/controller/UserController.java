package com.example.tableaudemov2.controller;

import com.example.tableaudemov2.dto.UserRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class UserController {

    @PostMapping("/users")
    public String createUser(@Valid @RequestBody UserRequest req) {
        log.info("Received request: {}", req);
        return "OK";
    }
}
