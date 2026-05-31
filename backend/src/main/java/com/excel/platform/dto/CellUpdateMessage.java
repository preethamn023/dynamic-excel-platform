package com.excel.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CellUpdateMessage {
    private Long sheetId;
    private String cellRef;
    private Integer rowIdx;
    private Integer colIdx;
    private String rawValue;
    private String calculatedValue;
    private String formulaExpression;
    private String formulaStatus;
    private String updatedBy;
    private String timestamp;
}
