package com.example.tableaudemov2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class TableauDemoV2ApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void forgetPassword() {
        System.out.println(new BCryptPasswordEncoder().encode("Test1234!"));
    }

}
