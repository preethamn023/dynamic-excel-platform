package com.excel.platform.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "data_validations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Sheet sheet;

    @Column(nullable = false)
    private Integer firstRow;

    @Column(nullable = false)
    private Integer lastRow;

    @Column(nullable = false)
    private Integer firstCol;

    @Column(nullable = false)
    private Integer lastCol;

    @Column(length = 20)
    private String validationType; // LIST, INTEGER, DECIMAL, DATE, TIME, TEXT_LENGTH, FORMULA, ANY

    @Column(columnDefinition = "TEXT")
    private String formula1; // For LIST: comma-separated values like "Yes,No" or cell range

    @Column(columnDefinition = "TEXT")
    private String formula2; // For range validations (max value)

    @Column
    private Boolean showDropdown;
}
