package com.example.novaledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.novaledger")
public class NovaLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovaLedgerApplication.class, args);
        System.out.println("Successfully started NovaLedger!");
    }

}
