package com.morapack.skyroute.setup.controller;

import com.morapack.skyroute.setup.dto.SetupRequest;
import com.morapack.skyroute.setup.dto.SetupResponse;
import com.morapack.skyroute.setup.service.SetupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final SetupService setupService;

    public SetupController(SetupService setupService) {
        this.setupService = setupService;
    }

    @PostMapping
    public ResponseEntity<SetupResponse> load(@RequestBody SetupRequest request) {
        SetupResponse response = setupService.load(request);
        return ResponseEntity.ok(response);
    }
}
