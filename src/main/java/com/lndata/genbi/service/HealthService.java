package com.lndata.genbi.service;

import com.lndata.genbi.model.dto.SystemModuleDto;
import com.lndata.genbi.model.entity.SystemModuleEntity;
import com.lndata.genbi.model.response.HealthResponse;
import com.lndata.genbi.repository.SystemModuleRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

  private final String applicationName;
  private final SystemModuleRepository systemModuleRepository;

  public HealthService(@Value("${spring.application.name}") String applicationName,
      SystemModuleRepository systemModuleRepository) {
    this.applicationName = applicationName;
    this.systemModuleRepository = systemModuleRepository;
  }

  public HealthResponse getHealth() {
    List<SystemModuleDto> modules =
        systemModuleRepository.findAll().stream().map(this::toDto).toList();

    return new HealthResponse(applicationName, "UP", modules.size(), modules);
  }

  private SystemModuleDto toDto(SystemModuleEntity entity) {
    return new SystemModuleDto(entity.getId(), entity.getCode(), entity.getName(),
        entity.getDescription());
  }
}
