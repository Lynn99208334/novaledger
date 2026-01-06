package com.example.tableaudemov2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.tableaudemov2")
public class TableauDemoV2Application {

    public static void main(String[] args) {
        SpringApplication.run(TableauDemoV2Application.class, args);
        System.out.println("Successfully started Spring! ");
        System.out.println("This is TableauDemoV2 !!!");
    }

}
