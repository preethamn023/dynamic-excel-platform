package com.excel.platform.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cell_styles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CellStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workbook_version_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private WorkbookVersion workbookVersion;

    @Column(nullable = false)
    private String styleHash; // Used for deduplication

    private String fontName;
    private Integer fontSize;
    private Boolean isBold;
    private Boolean isItalic;
    
    private String backgroundColor;
    private String borderTop;
    private String borderBottom;
    private String borderLeft;
    private String borderRight;
    private String numberFormat;
}
