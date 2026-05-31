package com.excel.platform.service;

import com.excel.platform.model.Workbook;
import com.excel.platform.model.WorkbookVersion;
import com.excel.platform.model.Sheet;
import com.excel.platform.model.Cell;
import com.excel.platform.model.CellStyle;
import com.excel.platform.model.MergedRegion;
import com.excel.platform.model.NamedRange;
import com.excel.platform.model.VersionStatus;
import com.excel.platform.model.CellDataType;
import com.excel.platform.model.FormulaStatus;
import com.excel.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final WorkbookRepository workbookRepository;
    private final WorkbookVersionRepository versionRepository;
    private final SheetRepository sheetRepository;
    private final CellRepository cellRepository;
    private final NamedRangeRepository namedRangeRepository;
    private final MergedRegionRepository mergedRegionRepository;
    private final CellStyleRepository cellStyleRepository;
    private final FormulaDependencyBuilder formulaDependencyBuilder;

    @Transactional
    public Workbook importExcelFile(MultipartFile file, String username) throws IOException {
        String fileName = file.getOriginalFilename();

        Workbook workbookEntity = Workbook.builder()
                .fileName(fileName)
                .uploadDate(LocalDateTime.now())
                .currentVersion(1)
                .status("ACTIVE")
                .build();
        workbookEntity = workbookRepository.save(workbookEntity);

        WorkbookVersion version = WorkbookVersion.builder()
                .workbook(workbookEntity)
                .versionNumber(1)
                .status(VersionStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .createdBy(username)
                .fileData(null)
                .build();
        version = versionRepository.save(version);

        try (InputStream is = file.getInputStream();
             org.apache.poi.ss.usermodel.Workbook poiWorkbook = new XSSFWorkbook(is)) {

            FormulaEvaluator evaluator = poiWorkbook.getCreationHelper().createFormulaEvaluator();
            
            // Extract Named Ranges
            extractNamedRanges(poiWorkbook, version);
            
            // Deduplicate and Extract Styles
            Map<Short, CellStyle> styleMap = new HashMap<>();

            for (int i = 0; i < poiWorkbook.getNumberOfSheets(); i++) {
                org.apache.poi.ss.usermodel.Sheet poiSheet = poiWorkbook.getSheetAt(i);

                Sheet sheetEntity = Sheet.builder()
                        .workbookVersion(version)
                        .sheetName(poiSheet.getSheetName())
                        .sheetIndex(i)
                        .visibilityState(poiWorkbook.isSheetHidden(i) ? "HIDDEN" : "VISIBLE")
                        .build();
                sheetEntity = sheetRepository.save(sheetEntity);

                // Extract Merged Regions
                extractMergedRegions(poiSheet, sheetEntity);

                List<com.excel.platform.model.Cell> cellsToSave = new ArrayList<>();

                for (Row row : poiSheet) {
                    for (org.apache.poi.ss.usermodel.Cell poiCell : row) {
                        CellStyle cellStyle = getOrCreateStyle(poiCell.getCellStyle(), version, styleMap, poiWorkbook);
                        cellsToSave.add(extractCellData(poiCell, sheetEntity, evaluator, cellStyle));
                    }
                }
                // Batch save cells
                cellRepository.saveAll(cellsToSave);
                // Build formula dependencies for this sheet
                formulaDependencyBuilder.buildDependenciesForSheet(sheetEntity.getId());
            }
        } catch (Exception e) {
            log.error("Failed to parse workbook", e);
            version.setStatus(VersionStatus.FAILED);
            versionRepository.save(version);
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage());
        }
        return workbookEntity;
    }

    private void extractNamedRanges(org.apache.poi.ss.usermodel.Workbook poiWorkbook, WorkbookVersion version) {
        List<NamedRange> ranges = new ArrayList<>();
        for (Name name : poiWorkbook.getAllNames()) {
            ranges.add(NamedRange.builder()
                    .workbookVersion(version)
                    .name(name.getNameName())
                    .referenceExpression(name.getRefersToFormula())
                    .build());
        }
        if (!ranges.isEmpty()) {
            namedRangeRepository.saveAll(ranges);
        }
    }

    private void extractMergedRegions(org.apache.poi.ss.usermodel.Sheet poiSheet, Sheet sheetEntity) {
        List<MergedRegion> regions = new ArrayList<>();
        for (int i = 0; i < poiSheet.getNumMergedRegions(); i++) {
            CellRangeAddress address = poiSheet.getMergedRegion(i);
            regions.add(MergedRegion.builder()
                    .sheet(sheetEntity)
                    .firstRow(address.getFirstRow())
                    .lastRow(address.getLastRow())
                    .firstCol(address.getFirstColumn())
                    .lastCol(address.getLastColumn())
                    .build());
        }
        if (!regions.isEmpty()) {
            mergedRegionRepository.saveAll(regions);
        }
    }

    private CellStyle getOrCreateStyle(org.apache.poi.ss.usermodel.CellStyle poiStyle, WorkbookVersion version, Map<Short, CellStyle> styleMap, org.apache.poi.ss.usermodel.Workbook poiWorkbook) {
        if (poiStyle == null) return null;
        short styleIndex = poiStyle.getIndex();
        if (styleMap.containsKey(styleIndex)) {
            return styleMap.get(styleIndex);
        }

        // Create style hash for deduplication logic if needed, but here we just rely on POI's internal index per workbook
        String styleHash = "STYLE_" + styleIndex;
        
        Font font = poiWorkbook.getFontAt(poiStyle.getFontIndex());

        CellStyle cellStyle = CellStyle.builder()
                .workbookVersion(version)
                .styleHash(styleHash)
                .fontName(font.getFontName())
                .fontSize((int) font.getFontHeightInPoints())
                .isBold(font.getBold())
                .isItalic(font.getItalic())
                .borderTop(poiStyle.getBorderTop().name())
                .borderBottom(poiStyle.getBorderBottom().name())
                .borderLeft(poiStyle.getBorderLeft().name())
                .borderRight(poiStyle.getBorderRight().name())
                .numberFormat(poiStyle.getDataFormatString())
                .build();
                
        cellStyle = cellStyleRepository.save(cellStyle);
        styleMap.put(styleIndex, cellStyle);
        return cellStyle;
    }

    private com.excel.platform.model.Cell extractCellData(org.apache.poi.ss.usermodel.Cell poiCell, Sheet sheetEntity, FormulaEvaluator evaluator, CellStyle style) {
        CellReference cellRef = new CellReference(poiCell.getRowIndex(), poiCell.getColumnIndex());

        com.excel.platform.model.Cell cellEntity = com.excel.platform.model.Cell.builder()
                .sheet(sheetEntity)
                .rowIdx(poiCell.getRowIndex())
                .colIdx(poiCell.getColumnIndex())
                .cellRef(cellRef.formatAsString())
                .formulaStatus(FormulaStatus.NONE)
                .style(style)
                .build();

        switch (poiCell.getCellType()) {
            case FORMULA:
                cellEntity.setFormulaExpression("=" + poiCell.getCellFormula());
                cellEntity.setDataType(CellDataType.FORMULA);
                try {
                    CellValue cellValue = evaluator.evaluate(poiCell);
                    cellEntity.setFormulaStatus(FormulaStatus.SUPPORTED);
                    cellEntity.setCalculatedValue(getCellValueAsString(cellValue));
                } catch (Exception e) {
                    log.warn("Failed to evaluate formula in cell {}: {}", cellRef.formatAsString(), e.getMessage());
                    cellEntity.setFormulaStatus(FormulaStatus.UNSUPPORTED);
                    cellEntity.setCalculatedValue(null);
                }
                break;
            case STRING:
                cellEntity.setRawValue(poiCell.getStringCellValue());
                cellEntity.setDataType(CellDataType.STRING);
                break;
            case NUMERIC:
                cellEntity.setRawValue(String.valueOf(poiCell.getNumericCellValue()));
                cellEntity.setDataType(CellDataType.NUMERIC);
                break;
            case BOOLEAN:
                cellEntity.setRawValue(String.valueOf(poiCell.getBooleanCellValue()));
                cellEntity.setDataType(CellDataType.BOOLEAN);
                break;
            case BLANK:
                cellEntity.setDataType(CellDataType.BLANK);
                break;
            case ERROR:
                cellEntity.setRawValue(String.valueOf(poiCell.getErrorCellValue()));
                cellEntity.setDataType(CellDataType.ERROR);
                break;
            default:
                cellEntity.setDataType(CellDataType.UNKNOWN);
        }
        return cellEntity;
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
