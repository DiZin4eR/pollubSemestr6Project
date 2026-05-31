package com.ecodatahub.gus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ecodatahub.gus.domain.GUSDataAttribute;

public interface GUSDataAttributeRepository extends JpaRepository<GUSDataAttribute, Integer> {
}
