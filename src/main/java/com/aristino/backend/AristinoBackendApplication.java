package com.aristino.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // bat tac vu dinh ky (tu dong huy don qua han thanh toan)
public class AristinoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AristinoBackendApplication.class, args);
    }

}