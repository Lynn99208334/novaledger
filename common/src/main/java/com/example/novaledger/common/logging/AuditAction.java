package com.example.novaledger.common.logging;

/**
 * 統一 Audit Log / Application Log 的 action 常數。
 *
 * 命名規則：動詞_名詞，全大寫。
 * 格式規範：action=XXX entity=XXX entityId=XXX result=SUCCESS/FAILED [reason=XXX]
 *
 * 範例：
 *   action=CREATE_ACCOUNT entity=Account entityId=123 result=SUCCESS
 *   action=DELETE_TRANSACTION entity=Transaction entityId=999 result=FAILED reason=NOT_FOUND
 */
public final class AuditAction {

    private AuditAction() {}

    // ── Auth ──────────────────────────────────────────
    public static final String LOGIN                = "LOGIN";
    public static final String LOGOUT               = "LOGOUT";
    public static final String REGISTER             = "REGISTER";
    public static final String VERIFY_EMAIL         = "VERIFY_EMAIL";
    public static final String RESEND_VERIFY        = "RESEND_VERIFY";

    // ── Account ───────────────────────────────────────
    public static final String CREATE_ACCOUNT       = "CREATE_ACCOUNT";
    public static final String UPDATE_ACCOUNT       = "UPDATE_ACCOUNT";
    public static final String DELETE_ACCOUNT       = "DELETE_ACCOUNT";

    // ── Credit Card ───────────────────────────────────
    public static final String CREATE_CARD          = "CREATE_CARD";
    public static final String UPDATE_CARD          = "UPDATE_CARD";
    public static final String DELETE_CARD          = "DELETE_CARD";

    // ── Transaction ───────────────────────────────────
    public static final String CREATE_TRANSACTION   = "CREATE_TRANSACTION";
    public static final String UPDATE_TRANSACTION   = "UPDATE_TRANSACTION";
    public static final String DELETE_TRANSACTION   = "DELETE_TRANSACTION";

    // ── Import ────────────────────────────────────────
    public static final String UPLOAD_FILE          = "UPLOAD_FILE";
    public static final String PARSE_FILE           = "PARSE_FILE";
    public static final String CONFIRM_IMPORT       = "CONFIRM_IMPORT";

    // ── Exchange Rate ─────────────────────────────────
    public static final String UPDATE_EXCHANGE_RATE = "UPDATE_EXCHANGE_RATE";

    // ── Admin ─────────────────────────────────────────
    public static final String CREATE_BANK          = "CREATE_BANK";
    public static final String UPDATE_BANK          = "UPDATE_BANK";
    public static final String UPDATE_USER_PLAN     = "UPDATE_USER_PLAN";

    // ── HTTP (Controller layer) ───────────────────────
    public static final String HTTP_REQUEST         = "HTTP_REQUEST";
    public static final String HTTP_RESPONSE        = "HTTP_RESPONSE";
    public static final String HTTP_ERROR           = "HTTP_ERROR";
}
