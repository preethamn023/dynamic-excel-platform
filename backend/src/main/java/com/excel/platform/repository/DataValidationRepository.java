package com.excel.platform.repository;

import com.excel.platform.model.DataValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataValidationRepository extends JpaRepository<DataValidation, Long> {
    List<DataValidation> findBySheetId(Long sheetId);
}
