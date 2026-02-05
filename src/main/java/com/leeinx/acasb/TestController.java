package com.leeinx.acasb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class TestController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/testPython")
    public String test() {
        String url = "http://localhost:5000/health";
        return restTemplate.getForObject(url, String.class);
    }
}