package com.example.s3webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class S3WebappApplication {

    public static void main(String[] args) {
        SpringApplication.run(S3WebappApplication.class, args);
    }
}
