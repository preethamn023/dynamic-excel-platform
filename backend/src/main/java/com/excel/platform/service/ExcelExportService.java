package com.excel.platform.service;

import com.excel.platform.model.*;
import com.excel.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    private final WorkbookVersionRepository versionRepository;
    private final SheetRepository sheetRepository;
    private final CellRepository cellRepository;
    private final CellStyleRepository cellStyleRepository;
    private final MergedRegionRepository mergedRegionRepository;

    public byte[] exportWorkbook(Long workbookId) {
        // 1. Find latest WorkbookVersion
        WorkbookVersion latestVersion = versionRepository.findTopByWorkbookIdOrderByVersionNumberDesc(workbookId)
                .orElseThrow(() -> new RuntimeException("Version not found for workbook: " + workbookId));

        // 2. Get all sheets for that version
        List<Sheet> sheets = sheetRepository.findByWorkbookVersionId(latestVersion.getId());

        try (XSSFWorkbook poiWorkbook = new XSSFWorkbook()) {
            // 3 & 4. For each sheet: create POI sheet, populate cells
            for (Sheet dbSheet : sheets) {
                org.apache.poi.ss.usermodel.Sheet poiSheet = poiWorkbook.createSheet(dbSheet.getSheetName());

                // Get all cells from DB for this sheet
                List<Cell> cells = cellRepository.findBySheetId(dbSheet.getId());

                for (Cell dbCell : cells) {
                    Row poiRow = poiSheet.getRow(dbCell.getRowIdx());
                    if (poiRow == null) {
                        poiRow = poiSheet.createRow(dbCell.getRowIdx());
                    }
                    org.apache.poi.ss.usermodel.Cell poiCell = poiRow.createCell(dbCell.getColIdx());

                    if (dbCell.getDataType() == null || dbCell.getDataType() == CellDataType.BLANK) {
                        poiCell.setBlank();
                    } else if (dbCell.getDataType() == CellDataType.FORMULA) {
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
                    } else if (dbCell.getDataType() == CellDataType.BOOLEAN) {
                        poiCell.setCellValue(Boolean.parseBoolean(dbCell.getRawValue()));
                    } else {
                        // STRING, ERROR, UNKNOWN
                        poiCell.setCellValue(dbCell.getRawValue() != null ? dbCell.getRawValue() : "");
                    }
                }

                // 5. Apply merged regions from DB
                List<MergedRegion> mergedRegions = mergedRegionRepository.findBySheetId(dbSheet.getId());
                for (MergedRegion mr : mergedRegions) {
                    poiSheet.addMergedRegion(new CellRangeAddress(
                            mr.getFirstRow(), mr.getLastRow(),
                            mr.getFirstCol(), mr.getLastCol()));
                }
            }

            // 6. Write to ByteArrayOutputStream, return bytes
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                poiWorkbook.write(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            log.error("Failed to export workbook {}: {}", workbookId, e.getMessage(), e);
            throw new RuntimeException("Failed to export workbook: " + e.getMessage(), e);
        }
    }
}
