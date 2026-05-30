package com.excel.platform.repository;

import com.excel.platform.model.MergedRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MergedRegionRepository extends JpaRepository<MergedRegion, Long> {
    List<MergedRegion> findBySheetId(Long sheetId);
}
