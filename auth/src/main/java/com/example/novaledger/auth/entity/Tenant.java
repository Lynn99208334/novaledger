package com.example.novaledger.auth.entity;

import com.example.novaledger.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "tenants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenants_slug", columnNames = "slug")
        }
)
public class Tenant extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

