package com.excel.platform.repository;

import com.excel.platform.model.Sheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SheetRepository extends JpaRepository<Sheet, Long> {
    List<Sheet> findByWorkbookVersionId(Long workbookVersionId);
}
