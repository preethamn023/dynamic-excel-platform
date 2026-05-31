package com.excel.platform.service;

import com.excel.platform.model.*;
import com.excel.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkbookService {

    private final WorkbookRepository workbookRepository;
    private final WorkbookVersionRepository workbookVersionRepository;
    private final SheetRepository sheetRepository;
    private final CellRepository cellRepository;
    private final FormulaDependencyRepository formulaDependencyRepository;
    private final AuditLogRepository auditLogRepository;
    private final MergedRegionRepository mergedRegionRepository;
    private final NamedRangeRepository namedRangeRepository;
    private final CellStyleRepository cellStyleRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Transactional
    public Workbook createEmptyWorkbook(String name) {
        // 1. Create and save Workbook entity
        Workbook workbook = Workbook.builder()
                .fileName(name)
                .uploadDate(LocalDateTime.now())
                .currentVersion(1)
                .status("ACTIVE")
                .build();
        workbook = workbookRepository.save(workbook);

        // 2. Create and save WorkbookVersion
        WorkbookVersion version = WorkbookVersion.builder()
                .workbook(workbook)
                .versionNumber(1)
                .status(VersionStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .createdBy("Anonymous")
                .fileData(null)
                .build();
        version = workbookVersionRepository.save(version);

        // 3. Create and save Sheet
        Sheet sheet = Sheet.builder()
                .workbookVersion(version)
                .sheetName("Sheet1")
                .sheetIndex(0)
                .visibilityState("VISIBLE")
                .build();
        sheet = sheetRepository.save(sheet);

        // 4. Create 30 rows x 10 columns of empty Cell entities
        List<Cell> cells = new ArrayList<>();
        for (int row = 0; row < 30; row++) {
            for (int col = 0; col < 10; col++) {
                CellReference ref = new CellReference(row, col);
                cells.add(Cell.builder()
                        .sheet(sheet)
                        .rowIdx(row)
                        .colIdx(col)
                        .cellRef(ref.formatAsString())
                        .dataType(CellDataType.BLANK)
                        .formulaStatus(FormulaStatus.NONE)
                        .build());
            }
        }

        // 5. Batch save all cells
        cellRepository.saveAll(cells);

        // 6. Broadcast workbook list change
        webSocketNotificationService.broadcastWorkbookListChange();

        log.info("Created empty workbook '{}' with id {}", name, workbook.getId());

        // 7. Return workbook
        return workbook;
    }

    @Transactional
    public void deleteWorkbook(Long id) {
        // Delete audit logs FIRST (they reference both workbook and sheet)
        auditLogRepository.deleteAll(auditLogRepository.findByWorkbookIdOrderByModifiedDateDesc(id));

        // Get all versions for this workbook
        List<WorkbookVersion> versions = workbookVersionRepository.findByWorkbookIdOrderByVersionNumberDesc(id);

        for (WorkbookVersion version : versions) {
            List<Sheet> sheets = sheetRepository.findByWorkbookVersionId(version.getId());
            for (Sheet sheet : sheets) {
                // Delete formula dependencies for this sheet
                formulaDependencyRepository.deleteBySourceCellSheetId(sheet.getId());
                // Delete cells for this sheet
                List<Cell> cells = cellRepository.findBySheetId(sheet.getId());
                cellRepository.deleteAll(cells);
                // Delete merged regions for this sheet
                mergedRegionRepository.deleteAll(mergedRegionRepository.findBySheetId(sheet.getId()));
            }
            // Delete named ranges for this version
            namedRangeRepository.deleteAll(namedRangeRepository.findByWorkbookVersionId(version.getId()));
            // Delete cell styles for this version
            cellStyleRepository.deleteAll(cellStyleRepository.findByWorkbookVersionId(version.getId()));
            // Delete sheets for this version
            sheetRepository.deleteAll(sheets);
        }

        // Delete versions
        workbookVersionRepository.deleteAll(versions);

        // Delete the workbook itself
        workbookRepository.deleteById(id);

        // Broadcast workbook list change
        webSocketNotificationService.broadcastWorkbookListChange();

        log.info("Deleted workbook with id {}", id);
    }
}
