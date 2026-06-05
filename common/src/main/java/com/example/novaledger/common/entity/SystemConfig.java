package com.example.novaledger.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_configs")
@Getter
@NoArgsConstructor
public class SystemConfig {

    @Id
    @Column(name = "config_key", length = 100, nullable = false)
    private String configKey;

    @Column(name = "config_value", length = 500, nullable = false)
    private String configValue;

    @Column(name = "value_type", length = 20, nullable = false)
    private String valueType;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    public void updateValue(String newValue, String updatedBy) {
        this.configValue = newValue;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
