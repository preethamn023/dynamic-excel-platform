package com.excel.platform.repository;

import com.excel.platform.model.NamedRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NamedRangeRepository extends JpaRepository<NamedRange, Long> {
    List<NamedRange> findByWorkbookVersionId(Long workbookVersionId);
}
