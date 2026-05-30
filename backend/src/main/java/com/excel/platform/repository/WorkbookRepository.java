package com.excel.platform.repository;

import com.excel.platform.model.Workbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkbookRepository extends JpaRepository<Workbook, Long> {
}
