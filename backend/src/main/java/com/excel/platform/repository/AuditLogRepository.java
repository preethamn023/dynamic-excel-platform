package com.excel.platform.repository;

import com.excel.platform.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByWorkbookIdOrderByModifiedDateDesc(Long workbookId);
}
