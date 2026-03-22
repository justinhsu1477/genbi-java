package com.nlq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 資料庫 Profile — 每個 Profile 代表一組 DB 連線 + DDL + Hints
 */
@Entity
@Table(name = "db_profiles", indexes = {
        @Index(name = "idx_profile_name", columnList = "profile_name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_name", nullable = false, unique = true, length = 128)
    private String profileName;

    @Column(name = "conn_name", length = 128)
    private String connName;

    @Column(name = "db_type", nullable = false, length = 32)
    private String dbType;

    @Column(name = "db_url", nullable = false, length = 512)
    private String dbUrl;

    @Column(name = "db_username", length = 128)
    private String dbUsername;

    @Column(name = "db_password", length = 256)
    private String dbPassword;

    @Column(name = "tables_info", columnDefinition = "TEXT")
    private String tablesInfo;

    @Column(name = "hints", columnDefinition = "TEXT")
    private String hints;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    /** RLS 開關 */
    @Column(name = "rls_enabled")
    @Builder.Default
    private Boolean rlsEnabled = false;

    /** RLS 設定（YAML 格式） */
    @Column(name = "rls_config", columnDefinition = "TEXT")
    private String rlsConfig;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
