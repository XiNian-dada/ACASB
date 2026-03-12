package com.leeinx.acasb;

import com.leeinx.acasb.config.PythonServiceProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class TestController {
    private final RestTemplate restTemplate;
    private final PythonServiceProperties pythonServiceProperties;

    public TestController(RestTemplate restTemplate, PythonServiceProperties pythonServiceProperties) {
        this.restTemplate = restTemplate;
        this.pythonServiceProperties = pythonServiceProperties;
    }

    @GetMapping("/testPython")
    public String test() {
        return restTemplate.getForObject(pythonServiceProperties.buildUrl("/health"), String.class);
    }
}
