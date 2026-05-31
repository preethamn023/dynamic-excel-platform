package com.excel.platform.service;

import com.excel.platform.dto.CellUpdateDto;
import com.excel.platform.dto.CellUpdateMessage;
import com.excel.platform.model.*;
import com.excel.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CellService {

    private final CellRepository cellRepository;
    private final SheetRepository sheetRepository;
    private final WorkbookVersionRepository workbookVersionRepository;
    private final AuditLogRepository auditLogRepository;
    private final FormulaDependencyRepository formulaDependencyRepository;
    private final WorkbookRepository workbookRepository;
    private final FormulaDependencyBuilder formulaDependencyBuilder;
    private final WebSocketNotificationService webSocketNotificationService;

    @Transactional
    public List<Cell> updateCell(Long workbookId, Long sheetId, CellUpdateDto dto, String username) {
        try {
            return doUpdateCell(workbookId, sheetId, dto, username);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict on cell {}, retrying once", dto.getCellRef());
            // Retry once after reloading
            return doUpdateCell(workbookId, sheetId, dto, username);
        }
    }

    private List<Cell> doUpdateCell(Long workbookId, Long sheetId, CellUpdateDto dto, String username) {
        // 1. Find or create the cell
        Cell rootCell = cellRepository.findBySheetIdAndCellRef(sheetId, dto.getCellRef())
                .orElseGet(() -> createNewCell(sheetId, dto.getCellRef()));

        // 2. Log audit (old value -> new value)
        logAudit(rootCell, dto.getNewValue(), username);

        // 3. Update cell data
        if (dto.isFormula()) {
            rootCell.setFormulaExpression(dto.getNewValue());
            rootCell.setFormulaStatus(FormulaStatus.SUPPORTED);
            rootCell.setDataType(CellDataType.FORMULA);
            rootCell.setRawValue(null);
        } else {
            rootCell.setRawValue(dto.getNewValue());
            rootCell.setFormulaExpression(null);
            rootCell.setFormulaStatus(FormulaStatus.NONE);
            // Try parsing as number
            try {
                Double.parseDouble(dto.getNewValue());
                rootCell.setDataType(CellDataType.NUMERIC);
            } catch (NumberFormatException e) {
                rootCell.setDataType(CellDataType.STRING);
            }
        }

        // 4. Save the cell
        final Cell savedRootCell = cellRepository.save(rootCell);

        // 5. Recalculate formulas
        List<Cell> updatedCells = recalculateFormulas(sheetId, savedRootCell);

        // Make sure rootCell is included
        if (updatedCells.stream().noneMatch(c -> c.getId().equals(savedRootCell.getId()))) {
            updatedCells.add(0, savedRootCell);
        }

        // 6. Rebuild dependency graph for the sheet
        formulaDependencyBuilder.rebuildDependenciesForSheet(sheetId);

        // 7. Broadcast via WebSocket
        List<CellUpdateMessage> messages = updatedCells.stream()
                .map(cell -> CellUpdateMessage.builder()
                        .sheetId(sheetId)
                        .cellRef(cell.getCellRef())
                        .rowIdx(cell.getRowIdx())
                        .colIdx(cell.getColIdx())
                        .rawValue(cell.getRawValue())
                        .calculatedValue(cell.getCalculatedValue())
                        .formulaExpression(cell.getFormulaExpression())
                        .formulaStatus(cell.getFormulaStatus() != null ? cell.getFormulaStatus().name() : null)
                        .updatedBy(username)
                        .timestamp(LocalDateTime.now().toString())
                        .build())
                .collect(Collectors.toList());
        webSocketNotificationService.broadcastCellUpdate(workbookId, sheetId, messages);

        // 8. Return all updated cells
        return updatedCells;
    }

    public List<Cell> recalculateFormulas(Long sheetId, Cell changedCell) {
        List<Cell> allCells = cellRepository.findBySheetId(sheetId);
        List<Cell> modifiedCells = new ArrayList<>();

        try (XSSFWorkbook tempWorkbook = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet poiSheet = tempWorkbook.createSheet("Sheet1");

            // Populate all cells into the POI workbook
            for (Cell dbCell : allCells) {
                Row poiRow = poiSheet.getRow(dbCell.getRowIdx());
                if (poiRow == null) {
                    poiRow = poiSheet.createRow(dbCell.getRowIdx());
                }
                org.apache.poi.ss.usermodel.Cell poiCell = poiRow.createCell(dbCell.getColIdx());

                if (dbCell.getDataType() == null || dbCell.getDataType() == CellDataType.BLANK) {
                    // Leave empty
                    poiCell.setBlank();
                } else if (dbCell.getDataType() == CellDataType.FORMULA) {
                    // Set formula without leading '='
                    String formula = dbCell.getFormulaExpression();
                    if (formula != null && formula.startsWith("=")) {
                        formula = formula.substring(1);
                    }
                    if (formula != null && !formula.isEmpty()) {
                        poiCell.setCellFormula(formula);
                    }
                } else if (dbCell.getDataType() == CellDataType.NUMERIC) {
                    try {
                        poiCell.setCellValue(Double.parseDouble(dbCell.getRawValue()));
                    } catch (NumberFormatException e) {
                        poiCell.setCellValue(dbCell.getRawValue() != null ? dbCell.getRawValue() : "");
                    }
                } else {
                    // STRING, BOOLEAN, ERROR, UNKNOWN
                    poiCell.setCellValue(dbCell.getRawValue() != null ? dbCell.getRawValue() : "");
                }
            }

            // Create FormulaEvaluator and evaluate all
            FormulaEvaluator evaluator = tempWorkbook.getCreationHelper().createFormulaEvaluator();
            evaluator.clearAllCachedResultValues();
            try {
                evaluator.evaluateAll();
            } catch (Exception e) {
                log.warn("Error during evaluateAll, some formulas may not be evaluated: {}", e.getMessage());
            }

            // Read back calculated values for formula cells
            for (Cell dbCell : allCells) {
                if (dbCell.getDataType() == CellDataType.FORMULA) {
                    Row poiRow = poiSheet.getRow(dbCell.getRowIdx());
                    if (poiRow != null) {
                        org.apache.poi.ss.usermodel.Cell poiCell = poiRow.getCell(dbCell.getColIdx());
                        if (poiCell != null && poiCell.getCellType() == CellType.FORMULA) {
                            try {
                                String newCalcValue = getEvaluatedValue(poiCell);
                                String oldCalcValue = dbCell.getCalculatedValue();
                                if (oldCalcValue == null || !oldCalcValue.equals(newCalcValue)) {
                                    dbCell.setCalculatedValue(newCalcValue);
                                    dbCell.setFormulaStatus(FormulaStatus.SUPPORTED);
                                    cellRepository.save(dbCell);
                                    modifiedCells.add(dbCell);
                                }
                            } catch (Exception e) {
                                log.warn("Failed to read back formula result for cell {}: {}", dbCell.getCellRef(), e.getMessage());
                                dbCell.setFormulaStatus(FormulaStatus.ERROR);
                                dbCell.setCalculatedValue(null);
                                cellRepository.save(dbCell);
                                modifiedCells.add(dbCell);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during formula recalculation for sheet {}: {}", sheetId, e.getMessage(), e);
        }

        return modifiedCells;
    }

    private Cell createNewCell(Long sheetId, String cellRefStr) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new RuntimeException("Sheet not found: " + sheetId));
        CellReference ref = new CellReference(cellRefStr);

        Cell newCell = Cell.builder()
                .sheet(sheet)
                .rowIdx(ref.getRow())
                .colIdx((int) ref.getCol())
                .cellRef(cellRefStr)
                .dataType(CellDataType.BLANK)
                .formulaStatus(FormulaStatus.NONE)
                .build();
        return cellRepository.save(newCell);
    }

    private void logAudit(Cell cell, String newValue, String username) {
        String oldValue = cell.getFormulaStatus() != FormulaStatus.NONE
                ? cell.getFormulaExpression()
                : cell.getRawValue();

        AuditLog auditLog = AuditLog.builder()
                .workbook(cell.getSheet().getWorkbookVersion().getWorkbook())
                .sheet(cell.getSheet())
                .cellRef(cell.getCellRef())
                .oldValue(oldValue)
                .newValue(newValue)
                .modifiedBy(username)
                .modifiedDate(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    private String getEvaluatedValue(org.apache.poi.ss.usermodel.Cell poiCell) {
        switch (poiCell.getCachedFormulaResultType()) {
            case NUMERIC:
                return String.valueOf(poiCell.getNumericCellValue());
            case STRING:
                return poiCell.getStringCellValue();
            case BOOLEAN:
                return String.valueOf(poiCell.getBooleanCellValue());
            case ERROR:
                return String.valueOf(poiCell.getErrorCellValue());
            default:
                return "";
        }
    }
}
