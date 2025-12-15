package com.example.tableaudemov2.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_key", nullable = false, unique = true)
    private String code;

    @Column
    private String description;

}
