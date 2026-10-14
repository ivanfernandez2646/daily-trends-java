package com.ivanfernandez.dailytrends.apps.cmsbackend.status;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusGetController {

    @GetMapping("/status")
    public ResponseEntity<Void> status() {
        return ResponseEntity.ok().build();
    }
}
