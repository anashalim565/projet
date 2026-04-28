package com.aisav.api;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptHashTest {

    @Test
    void generateBcryptHash() {
        String hash = new BCryptPasswordEncoder().encode("admin123");
        System.out.println(hash);
    }
}