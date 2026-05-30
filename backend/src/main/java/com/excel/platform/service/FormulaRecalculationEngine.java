package com.excel.platform.service;

import com.excel.platform.dto.CellUpdateDto;
import com.excel.platform.model.Workbook;
import com.excel.platform.model.WorkbookVersion;
import com.excel.platform.model.Sheet;
import com.excel.platform.model.Cell;
import com.excel.platform.model.FormulaDependency;
import com.excel.platform.model.AuditLog;
import com.excel.platform.model.VersionStatus;
import com.excel.platform.model.CellDataType;
import com.excel.platform.model.FormulaStatus;
import com.excel.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormulaRecalculationEngine {

    private final WorkbookVersionRepository versionRepository;
    private final CellRepository cellRepository;
    private final AuditLogRepository auditLogRepository;
    private final FormulaDependencyRepository dependencyRepository;
    private final WorkbookRepository workbookRepository;

    @Transactional
    public List<Cell> updateCellAndRecalculate(Long workbookId, Long sheetId, CellUpdateDto updateDto, String username) throws Exception {
        WorkbookVersion latestVersion = versionRepository.findTopByWorkbookIdOrderByVersionNumberDesc(workbookId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        Cell rootCell = cellRepository.findBySheetIdAndCellRef(sheetId, updateDto.getCellRef())
                .orElseThrow(() -> new RuntimeException("Cell not found"));

        // Audit Logging
        logAudit(rootCell, updateDto.getNewValue(), username);

        // Update DB Root Cell
        updateCellData(rootCell, updateDto);

        // Fetch dependency graph for incremental recalcs
        Set<Cell> affectedCells = fetchAllDependents(rootCell);
        affectedCells.add(rootCell);

        byte[] updatedFileData;
        List<Cell> returnedCells = new ArrayList<>();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(latestVersion.getFileData());
             org.apache.poi.ss.usermodel.Workbook poiWorkbook = new XSSFWorkbook(bis)) {

            org.apache.poi.ss.usermodel.Sheet poiSheet = poiWorkbook.getSheetAt(rootCell.getSheet().getSheetIndex());
            FormulaEvaluator evaluator = poiWorkbook.getCreationHelper().createFormulaEvaluator();

            // 1. Update root cell in POI
            updatePoiCell(poiSheet, updateDto);

            // 2. Clear formula cache and evaluate strictly the affected cells
            evaluator.clearAllCachedResultValues();

            for (Cell dependent : affectedCells) {
                if (dependent.getFormulaStatus() != FormulaStatus.NONE && dependent.getFormulaStatus() != FormulaStatus.UNSUPPORTED) {
                    CellReference checkRef = new CellReference(dependent.getCellRef());
                    Row checkRow = poiSheet.getRow(checkRef.getRow());
                    if (checkRow != null) {
                        org.apache.poi.ss.usermodel.Cell checkPoiCell = checkRow.getCell(checkRef.getCol());
                        if (checkPoiCell != null && checkPoiCell.getCellType() == CellType.FORMULA) {
                            try {
                                CellValue cellValue = evaluator.evaluate(checkPoiCell);
                                String newCalcValue = getCellValueAsString(cellValue);
                                
                                if (!newCalcValue.equals(dependent.getCalculatedValue())) {
                                    dependent.setCalculatedValue(newCalcValue);
                                    dependent.setFormulaStatus(FormulaStatus.SUPPORTED);
                                    cellRepository.save(dependent);
                                    returnedCells.add(dependent);
                                }
                            } catch (Exception e) {
                                dependent.setFormulaStatus(FormulaStatus.ERROR);
                                cellRepository.save(dependent);
                                returnedCells.add(dependent);
                            }
                        }
                    }
                }
            }

            if (!returnedCells.contains(rootCell)) {
                returnedCells.add(rootCell);
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                poiWorkbook.write(bos);
                updatedFileData = bos.toByteArray();
            }
        }

        // Create new Version Snapshot
        createNewVersionSnapshot(workbookId, latestVersion, updatedFileData, username);

        return returnedCells;
    }

    private void updateCellData(Cell cell, CellUpdateDto dto) {
        if (dto.isFormula()) {
            cell.setFormulaExpression(dto.getNewValue());
            cell.setFormulaStatus(FormulaStatus.SUPPORTED);
            cell.setDataType(CellDataType.FORMULA);
            cell.setRawValue(null);
        } else {
            cell.setRawValue(dto.getNewValue());
            cell.setFormulaExpression(null);
            cell.setFormulaStatus(FormulaStatus.NONE);
            cell.setDataType(CellDataType.STRING); // simplified
        }
        cellRepository.save(cell);
    }

    private void updatePoiCell(org.apache.poi.ss.usermodel.Sheet poiSheet, CellUpdateDto dto) {
        CellReference ref = new CellReference(dto.getCellRef());
        Row poiRow = poiSheet.getRow(ref.getRow());
        if (poiRow == null) poiRow = poiSheet.createRow(ref.getRow());
        org.apache.poi.ss.usermodel.Cell poiCell = poiRow.getCell(ref.getCol());
        if (poiCell == null) poiCell = poiRow.createCell(ref.getCol());

        if (dto.isFormula()) {
            String formula = dto.getNewValue().startsWith("=") ? dto.getNewValue().substring(1) : dto.getNewValue();
            poiCell.setCellFormula(formula);
        } else {
            try {
                double numVal = Double.parseDouble(dto.getNewValue());
                poiCell.setCellValue(numVal);
            } catch (NumberFormatException e) {
                poiCell.setCellValue(dto.getNewValue());
            }
        }
    }

    private Set<Cell> fetchAllDependents(Cell sourceCell) {
        Set<Cell> allDependents = new HashSet<>();
        Queue<Cell> queue = new LinkedList<>();
        queue.add(sourceCell);

        while (!queue.isEmpty()) {
            Cell current = queue.poll();
            List<FormulaDependency> deps = dependencyRepository.findBySourceCellId(current.getId());
            for (FormulaDependency dep : deps) {
                if (allDependents.add(dep.getDependentCell())) {
                    queue.add(dep.getDependentCell());
                }
            }
        }
        return allDependents;
    }

    private void logAudit(Cell cell, String newValue, String username) {
        String oldValue = cell.getFormulaStatus() != FormulaStatus.NONE ? cell.getFormulaExpression() : cell.getRawValue();
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

    private void createNewVersionSnapshot(Long workbookId, WorkbookVersion latestVersion, byte[] data, String username) {
        Workbook workbook = workbookRepository.findById(workbookId).orElseThrow();
        workbook.setCurrentVersion(latestVersion.getVersionNumber() + 1);
        workbookRepository.save(workbook);

        WorkbookVersion newVersion = WorkbookVersion.builder()
                .workbook(workbook)
                .versionNumber(latestVersion.getVersionNumber() + 1)
                .status(VersionStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .createdBy(username)
                .fileData(data)
                .build();
        versionRepository.save(newVersion);
        
        // Mark previous as archived
        latestVersion.setStatus(VersionStatus.ARCHIVED);
        versionRepository.save(latestVersion);
    }

    private String getCellValueAsString(CellValue cellValue) {
        if (cellValue == null) return "";
        switch (cellValue.getCellType()) {
            case STRING: return cellValue.getStringValue();
            case NUMERIC: return String.valueOf(cellValue.getNumberValue());
            case BOOLEAN: return String.valueOf(cellValue.getBooleanValue());
            case ERROR: return String.valueOf(cellValue.getErrorValue());
            default: return "";
        }
    }
}
