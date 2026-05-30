package com.excel.platform.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sheets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workbook_version_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private WorkbookVersion workbookVersion;

    @Column(nullable = false)
    private String sheetName;

    @Column(nullable = false)
    private Integer sheetIndex;
    
    // Optional: store visibility status (e.g. VISIBLE, HIDDEN, VERY_HIDDEN)
    private String visibilityState;
}
