package com.nlq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NlqApplication {
    public static void main(String[] args) {
        SpringApplication.run(NlqApplication.class, args);
    }
}
