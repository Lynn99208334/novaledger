package com.example.novaledger.service;

import com.example.novaledger.auth.entity.Tenant;
import com.example.novaledger.auth.entity.UserTenant;
import com.example.novaledger.auth.enums.UserTenantStatus;
import com.example.novaledger.auth.repository.TenantRepository;
import com.example.novaledger.auth.repository.UserTenantRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.dto.tenant.TenantResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TenantSwitchService {

    public static final String SESSION_CURRENT_TENANT_ID = "CURRENT_TENANT_ID";

    private static final UserTenantStatus USER_TENANT_STATUS_ACTIVE = UserTenantStatus.ACTIVE;
    private static final String TENANT_STATUS_ACTIVE = "ACTIVE";
    private static final String TENANT_TYPE_PERSONAL = "PERSONAL";

    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;

    public TenantSwitchService(TenantRepository tenantRepository,
                               UserTenantRepository userTenantRepository) {
        this.tenantRepository = tenantRepository;
        this.userTenantRepository = userTenantRepository;
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> findMyTenants(Long userId, HttpSession session) {
        List<UserTenant> userTenants =
                userTenantRepository.findByUserIdAndStatusAndDeletedAtIsNull(
                        userId,
                        USER_TENANT_STATUS_ACTIVE
                );

        Long currentTenantId = getCurrentTenantIdFromSession(session);

        if (currentTenantId == null && !userTenants.isEmpty()) {
            currentTenantId = resolveDefaultTenantId(userTenants);

            if (currentTenantId != null) {
                session.setAttribute(SESSION_CURRENT_TENANT_ID, currentTenantId);
            }
        }

        List<TenantResponse> result = new ArrayList<>();

        for (UserTenant userTenant : userTenants) {
            Tenant tenant = userTenant.getTenant();

            if (tenant == null) {
                continue;
            }

            if (!TENANT_STATUS_ACTIVE.equals(tenant.getStatus())) {
                continue;
            }

            boolean current = currentTenantId != null && currentTenantId.equals(tenant.getId());

            result.add(toTenantResponse(tenant, userTenant, current));
        }

        return result;
    }

    @Transactional
    public TenantResponse switchTenant(Long userId, Long tenantId, HttpSession session) {
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.TENANT_ID_REQUIRED);
        }

        UserTenant userTenant = userTenantRepository
                .findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                        userId,
                        tenantId,
                        USER_TENANT_STATUS_ACTIVE
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_ACCESS_DENIED));

        Tenant tenant = tenantRepository
                .findByIdAndStatus(tenantId, TENANT_STATUS_ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));

        session.setAttribute(SESSION_CURRENT_TENANT_ID, tenant.getId());

        return toTenantResponse(tenant, userTenant, true);
    }

    private Long resolveDefaultTenantId(List<UserTenant> userTenants) {
        for (UserTenant userTenant : userTenants) {
            Tenant tenant = userTenant.getTenant();

            if (tenant == null) {
                continue;
            }

            if (!TENANT_STATUS_ACTIVE.equals(tenant.getStatus())) {
                continue;
            }

            if (TENANT_TYPE_PERSONAL.equals(tenant.getType())) {
                return tenant.getId();
            }
        }

        for (UserTenant userTenant : userTenants) {
            Tenant tenant = userTenant.getTenant();

            if (tenant == null) {
                continue;
            }

            if (!TENANT_STATUS_ACTIVE.equals(tenant.getStatus())) {
                continue;
            }

            return tenant.getId();
        }

        return null;
    }

    private TenantResponse toTenantResponse(Tenant tenant,
                                            UserTenant userTenant,
                                            boolean current) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getType(),
                null,
                current
        );
    }

    private Long getCurrentTenantIdFromSession(HttpSession session) {
        Object value = session.getAttribute(SESSION_CURRENT_TENANT_ID);

        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.valueOf((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}