package com.excel.platform.repository;

import com.excel.platform.model.Cell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CellRepository extends JpaRepository<Cell, Long> {
    List<Cell> findBySheetId(Long sheetId);
    
    Optional<Cell> findBySheetIdAndCellRef(Long sheetId, String cellRef);
    
    @Modifying
    @Query("DELETE FROM Cell c WHERE c.sheet.id IN (SELECT s.id FROM Sheet s WHERE s.workbookVersion.workbook.id = :workbookId)")
    void deleteByWorkbookId(Long workbookId);
}
