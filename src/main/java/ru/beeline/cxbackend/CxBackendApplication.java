/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@SpringBootApplication
public class CxBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CxBackendApplication.class, args);
    }

}
