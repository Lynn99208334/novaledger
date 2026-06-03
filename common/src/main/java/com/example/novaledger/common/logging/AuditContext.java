package com.example.novaledger.common.logging;

/**
 * ThreadLocal 存放 audit log 的 before_value。
 * Service 在執行寫入操作前，先將舊資料序列化後塞入，
 * AuditLogAspect 執行後從這裡取出寫入 audit_logs。
 *
 * 使用規則：
 * - 只有 UPDATE / DELETE 操作需要 set，CREATE 不需要。
 * - AuditLogAspect 負責在每次 proceed 後呼叫 clear()，不需要 Service 手動清。
 */
public class AuditContext {

    private static final ThreadLocal<String> beforeValue = new ThreadLocal<>();

    public static void setBeforeValue(String json) {
        beforeValue.set(json);
    }

    public static String getBeforeValue() {
        return beforeValue.get();
    }

    public static void clear() {
        beforeValue.remove();
    }
}
