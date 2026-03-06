package com.localsecurecam.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StreamController {

    @GetMapping("/camera/main")
    public String getCameraStream() {

        return "rtsp://192.168.31.196:554/";

    }
}