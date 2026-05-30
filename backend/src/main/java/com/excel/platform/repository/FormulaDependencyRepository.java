package com.excel.platform.repository;

import com.excel.platform.model.FormulaDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormulaDependencyRepository extends JpaRepository<FormulaDependency, Long> {
    List<FormulaDependency> findBySourceCellId(Long sourceCellId);
    List<FormulaDependency> findByDependentCellId(Long dependentCellId);
}
