package com.example.tableaudemov2.adapter.cache;

public final class RedisKeyGenerator {

    private RedisKeyGenerator() {}

    private static final String BLACKLIST_JTI = "blacklist:jti:";

    public static String blacklistJti(String jti) {
        return BLACKLIST_JTI + jti;
    }
}


