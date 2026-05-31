package com.excel.platform.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cells", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sheet_row_col", columnNames = {"sheet_id", "rowIdx", "colIdx"})
}, indexes = {
    @Index(name = "idx_sheet_cellref", columnList = "sheet_id, cellRef")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Sheet sheet;

    @Column(nullable = false)
    private Integer rowIdx;

    @Column(nullable = false)
    private Integer colIdx;

    @Column(nullable = false, length = 20)
    private String cellRef; // e.g., "A1", "Z100"

    // Raw value could be quite long if it's text
    @Column(columnDefinition="TEXT")
    private String rawValue;

    // The calculated value resulting from formula evaluation
    @Column(columnDefinition="TEXT")
    private String calculatedValue;

    // The original formula string (e.g. "SUM(A1:A10)")
    @Column(columnDefinition="TEXT")
    private String formulaExpression;

    @Enumerated(EnumType.STRING)
    private FormulaStatus formulaStatus;

    @Enumerated(EnumType.STRING)
    private CellDataType dataType; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private CellStyle style;

    @Version
    private Long version;
}
