package com.aisav.api.dev;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGenerator {
    public static void main(String[] args) {
        String hash = new BCryptPasswordEncoder().encode("admin123");
        System.out.println(hash);
    }
}