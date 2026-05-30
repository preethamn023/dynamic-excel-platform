package com.excel.platform.controller;

import com.excel.platform.model.AuditLog;
import com.excel.platform.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/history/{workbookId}")
    public ResponseEntity<List<AuditLog>> getAuditHistory(@PathVariable Long workbookId) {
        return ResponseEntity.ok(auditLogRepository.findByWorkbookIdOrderByModifiedDateDesc(workbookId));
    }
}
