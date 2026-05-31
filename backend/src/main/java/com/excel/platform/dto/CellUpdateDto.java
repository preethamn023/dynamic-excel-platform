package com.excel.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CellUpdateDto {
    private String cellRef;
    private String newValue;
    @JsonProperty("isFormula")
    private boolean formula;
}
