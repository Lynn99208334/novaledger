package com.example.novaledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.example.novaledger")
@EnableAsync
@EnableScheduling
public class NovaLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovaLedgerApplication.class, args);
        System.out.println("Successfully started NovaLedger!");
    }

}
