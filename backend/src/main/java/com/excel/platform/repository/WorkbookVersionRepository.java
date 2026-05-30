package com.excel.platform.repository;

import com.excel.platform.model.WorkbookVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkbookVersionRepository extends JpaRepository<WorkbookVersion, Long> {
    List<WorkbookVersion> findByWorkbookIdOrderByVersionNumberDesc(Long workbookId);
    Optional<WorkbookVersion> findTopByWorkbookIdOrderByVersionNumberDesc(Long workbookId);
}
