package com.excel.platform.controller;

import com.excel.platform.model.Sheet;
import com.excel.platform.model.Workbook;
import com.excel.platform.model.WorkbookVersion;
import com.excel.platform.repository.SheetRepository;
import com.excel.platform.repository.WorkbookRepository;
import com.excel.platform.repository.WorkbookVersionRepository;
import com.excel.platform.service.ExcelExportService;
import com.excel.platform.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/workbooks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WorkbookController {

    private final ExcelImportService importService;
    private final ExcelExportService exportService;
    private final WorkbookRepository workbookRepository;
    private final WorkbookVersionRepository versionRepository;
    private final SheetRepository sheetRepository;

    @PostMapping("/upload")
    public ResponseEntity<Workbook> uploadWorkbook(@RequestParam("file") MultipartFile file) {
        try {
            Workbook workbook = importService.importExcelFile(file, "Anonymous");
            return ResponseEntity.ok(workbook);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Workbook>> getAllWorkbooks() {
        return ResponseEntity.ok(workbookRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workbook> getWorkbook(@PathVariable Long id) {
        return workbookRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/sheets")
    public ResponseEntity<List<Sheet>> getWorkbookSheets(@PathVariable Long id) {
        // Find latest version
        WorkbookVersion latestVersion = versionRepository.findTopByWorkbookIdOrderByVersionNumberDesc(id)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        return ResponseEntity.ok(sheetRepository.findByWorkbookVersionId(latestVersion.getId()));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadWorkbook(@PathVariable Long id) {
        try {
            byte[] fileData = exportService.exportWorkbook(id);
            Workbook workbook = workbookRepository.findById(id).orElseThrow();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + workbook.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileData);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
