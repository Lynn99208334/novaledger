package com.example.novaledger.auth.entity;

import com.example.novaledger.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_code_tenant", columnNames = {"code", "tenant_id"})
)
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "is_system_role", nullable = false)
    private Boolean isSystemRole = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getTenantId() { return tenantId; }
    public Boolean getIsSystemRole() { return isSystemRole; }
    public Set<Permission> getPermissions() { return permissions; }
}
