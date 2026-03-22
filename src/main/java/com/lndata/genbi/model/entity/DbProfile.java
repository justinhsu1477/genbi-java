package com.lndata.genbi.model.entity;

import com.lndata.genbi.model.dto.ProfileRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class DbProfile extends BaseEntity {

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

    /** Prompt 模板 JSON — 對應 Python prompt_map (存 JSON 字串) */
    @Column(name = "prompt_map", columnDefinition = "TEXT")
    private String promptMap;

    @Column(name = "rls_enabled")
    private Boolean rlsEnabled = false;

    @Column(name = "rls_config", columnDefinition = "TEXT")
    private String rlsConfig;

    // --- Domain method ---

    /** 從 ProfileRequest 更新所有可修改欄位 */
    public void updateFrom(ProfileRequest request) {
        this.connName = request.connName();
        this.dbType = request.dbType();
        this.dbUrl = request.dbUrl();
        this.dbUsername = request.dbUsername();
        this.dbPassword = request.dbPassword();
        this.tablesInfo = request.tablesInfo();
        this.hints = request.hints();
        this.comments = request.comments();
        this.promptMap = request.promptMap();
        this.rlsEnabled = Boolean.TRUE.equals(request.rlsEnabled());
        this.rlsConfig = request.rlsConfig();
    }
}
