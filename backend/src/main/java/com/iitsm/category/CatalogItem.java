package com.iitsm.category;

import jakarta.persistence.*;

@Entity
@Table(name = "catalog_item")
public class CatalogItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String kind;
  public String name;
  public boolean active = true;
}
