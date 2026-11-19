package com.freightfox.dispatch.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Welcome to FreightFox Dispatch Load Balancer API! Application is running successfully.";
    }
}
