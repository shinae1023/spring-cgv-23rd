package com.ceos.voteservice;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class HomeController {
    @GetMapping("/")
    @Operation(summary = "스웨거테스트")
    public String home() {
        return "세오스 프백 합동 과제 서버입니다.";
    }
}