package com.example.novaledger.auth.entity;

import com.example.novaledger.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_permission_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_resource_action", columnNames = {"resource", "action"})
        }
)
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 255)
    private String description;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
}
