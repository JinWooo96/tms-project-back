package com.tmsproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // jpa auditing 기능을 사용한다.
@SpringBootApplication
public class TmsProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmsProjectApplication.class, args);
    }

}
