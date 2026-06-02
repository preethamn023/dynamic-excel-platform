package com.excel.platform.controller;

import com.excel.platform.model.DataValidation;
import com.excel.platform.repository.DataValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/validations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DataValidationController {

    private final DataValidationRepository dataValidationRepository;

    @GetMapping("/sheet/{sheetId}")
    public ResponseEntity<List<DataValidation>> getValidationsForSheet(@PathVariable Long sheetId) {
        return ResponseEntity.ok(dataValidationRepository.findBySheetId(sheetId));
    }
}
