package com.lndata.genbi.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "system_modules")
public class SystemModuleEntity extends BaseEntity {

  @Column(nullable = false, unique = true, length = 100)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 255)
  private String description;
}
