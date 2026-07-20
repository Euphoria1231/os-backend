package com.tsy.oa.flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FlowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowServiceApplication.class, args);
    }
}
