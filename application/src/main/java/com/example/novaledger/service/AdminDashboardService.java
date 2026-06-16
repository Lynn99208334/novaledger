package com.example.novaledger.service;

import com.example.novaledger.dto.AdminDashboardDto;
import com.example.novaledger.finance.importjob.repository.UploadJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;

@Service
public class AdminDashboardService {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);

    private final UploadJobRepository uploadJobRepository;
    private final DataSource dataSource;

    public AdminDashboardService(UploadJobRepository uploadJobRepository,
                                 DataSource dataSource) {
        this.uploadJobRepository = uploadJobRepository;
        this.dataSource = dataSource;
    }

    /**
     * 彙整 Admin Dashboard 系統健康指標。
     *
     * recentErrorLogCount 目前回傳 -1（表示 N/A），
     * 待 LYN-44（system_error_logs）完成後替換為實際查詢。
     */
    public AdminDashboardDto getDashboardStats() {
        AdminDashboardDto dto = new AdminDashboardDto();

        dto.setPendingJobCount(uploadJobRepository.countPendingJobs());

        // LYN-44 完成前暫時回傳 -1，前端顯示 N/A
        dto.setRecentErrorLogCount(-1);

        dto.setDbHealthy(pingDatabase());

        return dto;
    }

    private boolean pingDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2); // 2 秒 timeout
        } catch (Exception e) {
            log.warn("DB health check failed", e);
            return false;
        }
    }
}
