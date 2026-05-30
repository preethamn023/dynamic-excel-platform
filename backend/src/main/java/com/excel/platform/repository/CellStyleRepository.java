package com.excel.platform.repository;

import com.excel.platform.model.CellStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CellStyleRepository extends JpaRepository<CellStyle, Long> {
    List<CellStyle> findByWorkbookVersionId(Long workbookVersionId);
}
