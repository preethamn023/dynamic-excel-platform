package com.excel.platform.dto;

import lombok.Data;

@Data
public class CellUpdateDto {
    private String cellRef;
    private String newValue;
    private boolean isFormula;
}
