package com.aisav.api.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String privateKeyPath;
    private String publicKeyPath;
    private long accessExpiryMs;
    private long refreshExpiryMs;

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }

    public long getAccessExpiryMs() {
        return accessExpiryMs;
    }

    public void setAccessExpiryMs(long accessExpiryMs) {
        this.accessExpiryMs = accessExpiryMs;
    }

    public long getRefreshExpiryMs() {
        return refreshExpiryMs;
    }

    public void setRefreshExpiryMs(long refreshExpiryMs) {
        this.refreshExpiryMs = refreshExpiryMs;
    }
}