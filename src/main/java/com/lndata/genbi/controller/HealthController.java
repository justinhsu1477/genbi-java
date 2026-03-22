package com.lndata.genbi.controller;

import com.lndata.genbi.model.constant.ApiPath;
import com.lndata.genbi.model.response.BaseSingleResponse;
import com.lndata.genbi.model.response.HealthResponse;
import com.lndata.genbi.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.HEALTH_PATH)
public class HealthController {

  private final HealthService healthService;

  public HealthController(HealthService healthService) {
    this.healthService = healthService;
  }

  @GetMapping
  public BaseSingleResponse<HealthResponse> getHealth() {
    return BaseSingleResponse.success("Application is available", healthService.getHealth());
  }
}
