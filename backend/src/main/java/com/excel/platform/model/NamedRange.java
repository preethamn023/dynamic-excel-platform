package com.excel.platform.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "named_ranges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NamedRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workbook_version_id", nullable = false)
    private WorkbookVersion workbookVersion;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition="TEXT", nullable = false)
    private String referenceExpression;
}
