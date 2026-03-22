package com.lndata.genbi.model.response;

import com.lndata.genbi.model.dto.SystemModuleDto;
import java.util.List;

public record HealthResponse(String applicationName, String status, long totalModules,
    List<SystemModuleDto> modules) {
}
