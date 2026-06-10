package com.example.novaledger.common.util;

/**
 * 個資遮罩工具類。
 *
 * <p>集中管理所有敏感欄位的遮罩邏輯，避免散落在各個 Service / Aspect。
 * 目前實作：maskAccountNumber、maskPhone、maskEmail（後兩者預留）。
 *
 * <p>設計為靜態方法，不需要注入，任何層都可直接呼叫。
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
        // utility class，禁止實例化
    }

    /**
     * 遮罩銀行帳號，僅顯示後四碼。
     *
     * <p>格式：{@code ****5678}
     * <ul>
     *   <li>null → 回傳 null</li>
     *   <li>長度 &lt; 4 → 回傳原值（不足以取後四碼）</li>
     *   <li>正常長度 → {@code ****XXXX}</li>
     * </ul>
     *
     * @param accountNumber 明文帳號（已解密）
     * @return 遮罩後的帳號字串
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            return null;
        }
        if (accountNumber.length() < 4) {
            return accountNumber;
        }
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return "****" + last4;
    }

    /**
     * 遮罩手機號碼，顯示前三碼與後三碼。（預留）
     *
     * <p>格式：{@code 0912***678}
     * <ul>
     *   <li>null → 回傳 null</li>
     *   <li>長度 &lt; 7 → 回傳原值</li>
     *   <li>正常長度（10碼）→ {@code XXX***XXX}</li>
     * </ul>
     *
     * @param phone 明文手機號碼
     * @return 遮罩後的手機字串
     */
    public static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.length() < 7) {
            return phone;
        }
        String prefix = phone.substring(0, 3);
        String suffix = phone.substring(phone.length() - 3);
        return prefix + "***" + suffix;
    }

    /**
     * 遮罩 Email，保留首字與 domain。（預留）
     *
     * <p>格式：{@code l***@example.com}
     * <ul>
     *   <li>null → 回傳 null</li>
     *   <li>不含 @ → 回傳 {@code ***}</li>
     *   <li>local part 長度 &le; 1 → {@code X***@domain}</li>
     *   <li>正常 → 保留第一個字元，其餘遮罩 {@code X***@domain}</li>
     * </ul>
     *
     * @param email 明文 Email
     * @return 遮罩後的 Email 字串
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        if (!email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        return local.charAt(0) + "***@" + domain;
    }
}
