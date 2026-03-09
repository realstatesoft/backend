package com.openroof.openroof;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpenRoofApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenRoofApplication.class, args);
    }

}
