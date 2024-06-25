package com.example.rateLimiting.api;

import io.github.ujjawalgupta29.annotations.RateLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/helloApis")
@RateLimit(
        thresholdPerService = 5
)
public class HelloApis {
    @GetMapping(path = "/hello-world")
    @ResponseBody
    public String helloWorld() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "Hello World";
    }
}
