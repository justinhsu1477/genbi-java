package com.lndata.genbi.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Profile 建立/更新請求 DTO
 */
public record ProfileRequest(
        @NotBlank(message = "profileName 不能為空")
        String profileName,

        String connName,

        @NotBlank(message = "dbType 不能為空")
        String dbType,

        @NotBlank(message = "dbUrl 不能為空")
        String dbUrl,

        String dbUsername,
        String dbPassword,
        String tablesInfo,
        String hints,
        String comments,
        String promptMap,
        Boolean rlsEnabled,
        String rlsConfig
) {}
