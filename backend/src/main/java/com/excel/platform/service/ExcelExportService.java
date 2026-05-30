package com.excel.platform.service;

import com.excel.platform.model.WorkbookVersion;
import com.excel.platform.repository.WorkbookVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final WorkbookVersionRepository versionRepository;

    public byte[] exportWorkbook(Long workbookId) {
        WorkbookVersion latestVersion = versionRepository.findTopByWorkbookIdOrderByVersionNumberDesc(workbookId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        return latestVersion.getFileData();
    }
}
