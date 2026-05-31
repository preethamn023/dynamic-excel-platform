package com.excel.platform.repository;

import com.excel.platform.model.FormulaDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormulaDependencyRepository extends JpaRepository<FormulaDependency, Long> {
    List<FormulaDependency> findBySourceCellId(Long sourceCellId);
    List<FormulaDependency> findByDependentCellId(Long dependentCellId);

    @Modifying
    @Query("DELETE FROM FormulaDependency fd WHERE fd.sourceCell.sheet.id = :sheetId OR fd.dependentCell.sheet.id = :sheetId")
    void deleteBySourceCellSheetId(Long sheetId);
}
