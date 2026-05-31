package com.excel.platform.controller;

import com.excel.platform.dto.CellUpdateDto;
import com.excel.platform.model.Cell;
import com.excel.platform.repository.CellRepository;
import com.excel.platform.service.CellService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cells")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CellController {

    private final CellRepository cellRepository;
    private final CellService cellService;

    @GetMapping("/sheet/{sheetId}")
    public ResponseEntity<List<Cell>> getCellsForSheet(@PathVariable Long sheetId) {
        return ResponseEntity.ok(cellRepository.findBySheetId(sheetId));
    }

    @PutMapping("/update")
    public ResponseEntity<List<Cell>> updateCell(
            @RequestParam Long workbookId,
            @RequestParam Long sheetId,
            @RequestBody CellUpdateDto updateDto) {
        try {
            List<Cell> updatedCells = cellService.updateCell(workbookId, sheetId, updateDto, "Anonymous");
            return ResponseEntity.ok(updatedCells);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
