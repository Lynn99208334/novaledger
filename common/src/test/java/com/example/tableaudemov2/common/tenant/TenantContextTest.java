package com.example.tableaudemov2.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        // 保險起見，每個測試結束都清乾淨
        TenantContext.clear();
    }

    /**
     * 測試set / get 是否正常運作
     * */
    @Test
    void setAndGetTenantId_shouldReturnSameValue() {
        // given
        Long tenantId = 100L;

        // when
        TenantContext.setTenantId(tenantId);

        // then
        assertEquals(tenantId, TenantContext.getTenantId());
    }

    /**
     * 測試clear是否清乾記
     * */
    @Test
    void clear_shouldRemoveTenantId() {
        // given
        TenantContext.setTenantId(200L);

        // when
        TenantContext.clear();

        // then
        assertNull(TenantContext.getTenantId());
    }

    /**
     * 不同 thread 是否隔離
     * */
    @Test
    void tenantContext_shouldBeIsolatedBetweenThreads() throws InterruptedException {
        // given
        TenantContext.setTenantId(1L);

        final Long[] otherThreadTenantId = new Long[1];

        Thread thread = new Thread(() -> {
            // 在另一個 thread 裡，預設應該是 null
            otherThreadTenantId[0] = TenantContext.getTenantId();
        });

        // when
        thread.start();
        thread.join();

        // then
        assertNull(otherThreadTenantId[0]);
        assertEquals(1L, TenantContext.getTenantId());
    }
}
