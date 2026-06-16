package com.example.novaledger.dto;

import lombok.Data;

@Data
public class AdminDashboardDto {

    /** PENDING 狀態的匯入任務數量 */
    private long pendingJobCount;

    /**
     * 近 24 小時 error log 數量。
     * -1 表示尚未接上 system_error_logs（LYN-44），前端顯示 N/A。
     */
    private long recentErrorLogCount;

    /** DB 連線是否正常 */
    private boolean dbHealthy;
}
