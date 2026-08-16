package com.wb.culinaryCode.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserAuthenticationController {

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(){
        return ResponseEntity.ok("User logged in successfully!");
    }
}
