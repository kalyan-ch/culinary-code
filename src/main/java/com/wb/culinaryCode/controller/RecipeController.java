package com.wb.culinaryCode.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecipeController {

    @GetMapping("/")
    public ResponseEntity<String> getWelcomeMessage() {
        return ResponseEntity.ok("Welcome to Culinary Code!");
    }
}
