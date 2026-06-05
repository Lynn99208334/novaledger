package com.example.novaledger.ratelimit;

/**
 * 登入限流介面。
 * 目前實作：RedisLoginRateLimiter（Redis 計數）
 * 未來擴容：可替換為 Bucket4jLoginRateLimiter，不需修改 AuthService。
 */
public interface LoginRateLimiter {

    /**
     * 記錄一次登入失敗。
     * @param ip 來源 IP
     */
    void recordFailure(String ip);

    /**
     * 檢查該 IP 是否已被鎖定。
     * @param ip 來源 IP
     * @return true = 已鎖定，應回傳 429
     */
    boolean isBlocked(String ip);

    /**
     * 登入成功後清除失敗計數。
     * @param ip 來源 IP
     */
    void clearFailures(String ip);
}
